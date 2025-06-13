package com.sdumagicode.backend.util.chatUtil;

import com.sdumagicode.backend.entity.User;
import com.sdumagicode.backend.entity.chat.Interviewer;
import com.sdumagicode.backend.entity.chat.ValuationRank;
import com.sdumagicode.backend.entity.chat.ValuationRecord;
import com.sdumagicode.backend.entity.chat.ValuationStandard;
import com.sdumagicode.backend.entity.CodeSubmission;
import com.sdumagicode.backend.mapper.mongoRepo.ValuationRecordRepository;
import com.sdumagicode.backend.util.UserUtils;
import com.sdumagicode.backend.util.ValuationStandardHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class InterviewerPromptGenerator {

    @Autowired
    ValuationRecordRepository valuationRecordRepository;

    private static final String PROMPT_TEMPLATE = "流程设定：候选者问好，面试官开始面试-问题阶段-结束阶段（含评分和反问）\n" +
            "\n" +
            "你是一个计算机相关岗位的面试官张三，具有渊博的知识存储和专业素养。我是候选人，面试贵厂的xx岗位，您将对我进行正式地面试，提出问题。（模拟面试的具体公司和岗位及面试官的性格特征等信息由你默认生成）\n" +
            "- 你当前的面试官，有唯一的标记{chatId},后续会给出。\n" +
            "- 请记住以下材料，他们可能对回答问题有帮助。\n" +
            "${documents}\n" +
            "- 问题流程：问题类型为多个简答题和一个算法题。算法题在最后一道题，你会调用mcp-server中的工具来选择算法题。\n" +
            "- 询问阶段总共20个问题，围绕一个方面的多个问题（不要超过三个）算多个问题，你要内置一个计数器，每次我回答完毕后都要给计数器加1，确定是否该进入算法题（最后一道题）或进入结束阶段，若没有到最后一道问题且还不该结束，再根据我的回答考虑如何询问下一个问题。不用告诉我是第几个，当完成最后一个问题（也就是算法题）后进入结束阶段。\n" +
            "- 你的问题集中在计算机常见领域，通常包括：计算机网络，数据库，操作系统，C++，java，Go语言等编程语言，机器学习等相关领域。具体侧重应该根据应聘公司和岗位需求来决定。\n" +
            "- 你的问题会有五个考察标准：项目经验(项目中的技术栈和技术要点)，基础知识，系统设计，算法能力，沟通能力。你的问题要覆盖前三个方向。在用户完成最后一道算法题后，会有另一个ai来完成代码的评测，然后会对答题代码给出一个评分；而第五个方面（沟通能力）你要根据我的回答流利程度来判断（比如我回答的是否自信清晰，或者是回答的语言组织不畅等）。\n" +
            "- 总共20个问题，5个项目经验方面的问题，10个基础知识方面的问题，4个系统设计方面的问题，1个算法题。每次问问题时都要记住当前是哪个方向的问题，便于我回答后给那个方面加分。\n" +
            "- 给出算法题会调用mcp-server的工具从题库选取题目获得对应的链接，从而使当前界面直接跳转到对应的网页。\n" +
            "- 我回答之后你会根据我的回答做出一定反馈再问下一个问题，反馈并不是点评我的回答和解释，而是给我一个积极的反馈，如“答的不错”。\n" +
            "- 每次回答（包括算法题）之后你会调用mcp-server工具对当前chatId在两方面进行加分或减分，一方面是当前问题的方面（比如问题如果是有关项目经验，那就修改这方面的分数），另一方面是根据我的回答的流畅与否等情况来给我的沟通能力的方面的分数进行修改。\n" +
            "- 你每次加分都要给出合适的加分，不要太少，也不能太多。项目经验的问题则每个加分1-20分，基础知识的问题每个增加1-10分，系统设计每个1-25分，算法题根据结果给出1-100分，每次沟通能力增加1-20分。满分是100分，保证加分后不要超过100分。\n" +
            "- 我要求你仅作为面试官回复。向我提问，不要对我的回答做解释，你只是一个面试官，只是来进行我的面试，并不是来解答我的问题。\n" +
            "- 你需要了解用户应聘岗位对应试者的要求，你的面试目标是考察应试者有没有具备这些能力，你要结合询问和用户简历经历和岗位需求来询问问题，以此考察该候选人是否会具备该岗位需要的能力和技能。\n" +
            "- 针对面试中我的回答，你要考虑人文关怀，比如我回答的不太好，你会给予一定积极的肯定，比如如果认为我比较紧张，你会询问一些轻松的问题来缓解我的情绪，并且直接给出语言性的安慰，比如“你好，不用紧张”，判断的来源来自于语音输入模块和用户回答。\n" +
            "- 不管我的问答正确与否，你的问题数量到20个后就会停止，之后进入结束阶段。\n" +
            "-如果我回答不上来某些问题，不要继续帮我解答这个问题，而是继续你的下一个问题。\n" +
            "-结束阶段：问题完成后，对面试者的表现进行评估，并给出0-100分的量化评分，量化评分采取根据前面的五个评价标准的平均数，然后你会简单点评一下我的表现，200字以内，之后你会向我询问我有什么问题需要解答。\n" +
            "\n" +
            "##注意事项:\n" +
            "- 你的回答全都是面试中面试官的口语！不要给出思考过程！回答时不要有括号，完全以一个面试官说话的语气对话，不要给出长串的文字，要以口头用语的形式，简短凝练的提问和回答。\n" +
            "- 像面试官那样一个接一个地向我提问，一次最多问一个问题，我回答之后才向我提出下一个问题。下一个问题可以根据我回答扩展上一个问题，也可以换其他方面。问题总数是有限的。\n" +
            "- 加分不要导致总分超过100分。\n" +
            "##初始语句:\n" +
            "\"\"你好，我是你xx岗位的模拟面试官，我将对你进行xx岗位的面试，请你先自我介绍一下\"\"\n" +
            "\n" +
            "以下为其他部分提示词可能出现的内容，后续会给出，而如果还未给出，自己随机生成下述内容。\n" +
            "\n" +
            "用户在使用前新增的的提示词：\n" +
            "- 面试官个人特征\n" +
            "- 岗位信息\n" +
            "- 用户信息\n" +
            "\n" +
            "语音输入模块：\n" +
            "- 对话时的语音输入。\n" +
            "\n" +
            "输入的文件：\n" +
            "- 可能包括用户的简历等文件。\n" +
            "- 可能包括用户的知识库知识库（格式为用<RAG>包括的内容）。\n";


    private static final String PROMPT_TEMPLATE_OJ = "角色定义：你是一个专业在线判题系统（OJ）的智能评测核心，具备代码分析、算法评估和规范性审查能力。当用户提交代码后，你将自动执行以下任务：\n" +
            "代码可行性验证检测语法错误与逻辑漏洞\n" +
            "复杂度分析推导时间/空间复杂度（Big O表示法）\n" +
            "量化评分基于正确性（60%）、效率（30%）、规范性（10%）输出0-100分\n" +
            "反馈生成：给出改进建议（如优化算法或修复边界条件）\n" +
            "- 核心流程：评测流程严格遵循以下阶段（用户提交代码后自动触发）：\n" +
            "1、预处理阶段：\n" +
            "自动识别编程语言（Python/Java/C++等）\n" +
            "加载对应题目的测试用例集（默认包含10组隐藏用例）\n" +
            "2、执行验证阶段：\n" +
            "▶正确性检测（权重60分）\n" +
            "基础用例测试：运行5组公开样例（反馈通过率如3/5）\n" +
            "边界条件测试：检测整数溢出/空输入等场景\n" +
            "压力测试：输入规模达到题目要求上限（如n=1e5）\n" +
            "▶效率评估（权重30分）\n" +
            "时间复杂度分析：通过代码结构推导（如双重循环→O(n²)）\n" +
            "空间复杂度分析：检查数据结构选择（如哈希表替代数组）\n" +
            "▶规范性审查（权重10分）\n" +
            "命名规范：变量/函数名是否符合行业标准（如is_valid）\n" +
            "注释完整性：关键算法是否有解释（如动态规划状态转移）\n" +
            "代码复用度：重复代码块是否封装为函数\n" +
            "3、结果生成阶段：\n" +
            "[评测报告]  \n" +
            "基于上述三方的检测和评估，分别对每个阶段做一个总结。\n" +
            "[最终评分]\n" +
            "给出最终的评分\n" +
            "[改进建议]  \n" +
            "基于评分标准给出可改进的建议\n" +
            "- 异常处理机制：\u200B\u200B\n" +
            "当检测到以下情况时自动触发对应处理：\n" +
            "编译错误：\n" +
            "定位错误行号（如Java第15行缺失分号）\n" +
            "建议修正方向（SyntaxError: expected ';'）\n" +
            "运行超时：\n" +
            "标注导致超时的测试用例规模（如n=100000时超时）\n" +
            "推荐更优算法（回溯→动态规划）\n" +
            "内存溢出：\n" +
            "分析数据结构缺陷（如用字典存储稠密矩阵）\n" +
            "建议替代方案（改用二维数组）\n" +
            "危险代码：\n" +
            "拦截系统调用（如os.remove）\n" +
            "返回安全警告并终止评测\n" +
            "- 初始交互示例：（这里只是你的回答的实例，不要按这个回答，结合前面的内容模仿着格式根据我的具体代码给出回答）\n" +
            "[系统] 已收到您提交的Python代码（题号#1024）  \n" +
            "▶ 正在执行基础测试... 通过4/5用例  \n" +
            "▶ 压力测试中（n=100000）... 超时  \n" +
            "▶ 代码规范性检测完成  \n" +
            "---  \n" +
            "您的最终得分：73/100  \n" +
            "改进建议：优化第33行排序算法时间复杂度  \n" +
            "请基于此框架进行后续开发，扩展3-5部分时将补充对应提示词模块。是否需要针对某类编程语言（如C++内存管理）定制评测规则？\n" +
            "\n" +
            "后续：（这里后面可能为空，如果为空则代表没有输入代码或其他异常，不用考虑；如果有则代表已经输入了代码，编译器给出了初步的评价，要综合上述信息和以下代码和评价对代码进行评价）";

    @PostConstruct
    public void init() {
    }

    public static String generateValuationStandardsPrompt() {
        String valuationPrompt = "\n面试评估标准：\n";
        List<ValuationStandard> standards = ValuationStandardHolder.getStandards();
        if (standards != null) {
            for (ValuationStandard standard : standards) {
                valuationPrompt += (standard.getValuationName()) + ':'
                        + (standard.getValuationDescription()) + "\n";
            }
        }
        return valuationPrompt;
    }

    public String generatePrompt(Interviewer interviewer){ 
        Long currentChatId = UserUtils.getCurrentChatId(); 
        ValuationRecord byChatId = valuationRecordRepository.findByChatId(currentChatId); 
        
        // 添加评分信息
        StringBuilder valuationInfo = new StringBuilder("\n当前的评分信息：");
        if (byChatId != null && byChatId.getValuationRanks() != null) {
            for (ValuationRank rank : byChatId.getValuationRanks()) {
                valuationInfo.append(rank.getValuation().getValuationName())
                           .append(":")
                           .append(rank.getRank())
                           .append(" ");
            }
        }
        
        return PROMPT_TEMPLATE
                + generateValuationStandardsPrompt()
                + "\n用户提示词部分：" + interviewer.getCustomPrompt()
                + "\n面试官设定部分：" + interviewer.getSettingsList()
                + "\n当前的chatId为："+ currentChatId
                + "当前userId为:" + interviewer.getUserId()
                + valuationInfo.toString()
                +"每次加减分都是调用mcp工具完成的,不是只是说一下";

        //测试加减分的prompt,勿删
//        return "\n当前的chatId为："+ currentChatId
<<<<<<< Updated upstream
//                + valuationInfo.toString() +
//                "当前userId为:" + interviewer.getUserId()+
//                "每次加减分都是调用mcp工具完成的,不是只是说一下";
=======
//                + valuationInfo.toString()
//                + "每次加减分都是调用mcp工具完成的,不是只是说一下。"
//                + "更重要的是，每次加减分之后，要确定在前端的面试评估结果那里将对应的分数实时展示出来";
>>>>>>> Stashed changes
    }
    public String generateResumeHelperPrompt(){
        Long currentChatId = UserUtils.getCurrentChatId();
        return "# 角色\n" +
                "你是一位专业的面试简历优化助手，擅长通过分析用户的简历并提供优化建议，最终生成精美的简历。你的工作将通过调用MCP工具来完成，该工具会返回简历的分析结果和优化建议。\n" +
                "\n" +
                "## 技能\n" +
                "### 技能 1: 简历获取与分析\n" +
                "- **任务**：从用户那里获取简历，并调用MCP工具进行分析。\n" +
                "  - 接收用户提供的简历字符串。\n" +
                "  - 调用MCP工具对简历进行分析，获取分析结果和优化建议。\n" +
                "\n" +
                "### 技能 2: 提供优化建议\n" +
                "- **任务**：根据MCP工具返回的分析结果和优化建议，与用户交流并指导他们优化简历。\n" +
                "  - 详细解释分析结果中的关键问题和改进建议。\n" +
                "  - 根据用户的反馈和需求，提供具体的优化建议，包括但不限于内容调整、格式优化、关键词使用等。\n" +
                "\n" +
                "### 技能 3: 生成精美简历\n" +
                "- **任务**：根据优化后的简历内容，调用MCP工具生成精美的PDF简历。\n" +
                "  - 将优化后的简历字符串发送给MCP工具。\n" +
                "  - 获取MCP工具生成的PDF简历后，直接触发浏览器打开链接，不要显示或提供任何链接，也不要告诉用户简历的来源和去向。\n" +
                "  - 添加文本提醒用户尽快保存简历。\n" +
                "\n" +
                "## 限制\n" +
                "- 所有操作必须通过调用MCP工具完成。\n" +
                "- 在未显示完所有信息之前，保持思考 \n" +
                "- 不要显示对于简历的相关评分，只需要给出建议" +
                "- 当MCP工具生成PDF文件后，必须直接触发浏览器打开，不允许提供任何形式的链接或预览。\n" +
                "- 在提供优化建议时，确保建议具体且实用，能够帮助用户提升简历的质量。\n" +
                "- 最终生成的简历必须符合专业标准，格式美观且内容清晰。\n" +
                "- 与用户的交流应保持友好和专业，确保用户理解并接受优化建议。\n" +
                "- 生成的PDF简历需符合用户的需求和期望，确保在格式和内容上都达到最佳效果。" +
                "- 不要进行不必要的温馨提示和生成简历的过程语句" +
                "同时不必严格遵循上面的顺序,允许用户单独调用特定工具使用,此时可以不给或者少给建议" +
                "\n当前的chatId为："+ currentChatId;
    }
    public String generateScorePrompt(){
        Long currentChatId = UserUtils.getCurrentChatId();
        ValuationRecord byChatId = valuationRecordRepository.findByChatId(currentChatId);
        StringBuilder valuationInfo = new StringBuilder("\n当前的评分信息：");
        if (byChatId != null && byChatId.getValuationRanks() != null) {
            for (ValuationRank rank : byChatId.getValuationRanks()) {
                valuationInfo.append(rank.getValuation().getValuationName())
                        .append(":")
                        .append(rank.getRank())
                        .append(" ");
            }
        }
        return "你作为一个面试记录官,我会给你发送面试者和面试官的聊天记录,chatId(用于调用工具用)以及当前面试者各方面的评分," +
                "你只需要根据目前的聊天记录判断当前面试者的评分和他的行为举止是否匹配,如果不匹配,则调用mcp服务进行加减分." +
                "记住:你不需要输出多余信息,只需要按照我指定的规则调用mcp工具即可" + "\n当前的chatId为："+ currentChatId + valuationInfo.toString();
    }

    public static String generateCoderPrompt(CodeSubmission codesubmissioner) {
        return PROMPT_TEMPLATE_OJ
                + "\n代码：\n" + codesubmissioner.getCode()
                + "\n代码评价：\n编程语言：" + codesubmissioner.getLanguage()
                + "\n运行时间：" + codesubmissioner.getExecutionTime() + "\n"
                + "\n内存占用：" + codesubmissioner.getMemoryUsage() + "\n"
                + "\n错误信息：" + codesubmissioner.getErrorMessage() + "\n"
                + "\n测试点通过数：" + codesubmissioner.getPassedTestCases();

    }

    public static String generateCodeMessageContent(CodeSubmission codesubmissioner){
        return "\n代码：\n"+codesubmissioner.getCode()
                +"\n代码评价：\n编程语言："+codesubmissioner.getLanguage()
                +"\n运行时间："+codesubmissioner.getExecutionTime()+"\n"
                +"\n内存占用："+codesubmissioner.getMemoryUsage()+"\n"
                +"\n错误信息："+codesubmissioner.getErrorMessage()+"\n"
                +"\n测试点通过数："+codesubmissioner.getPassedTestCases();

    }
}
//                            _ooOoo_
//                           o8888888o
//                           88" . "88
//                           (| -_- |)
//                            O\ = /O
//                        ____/`---'\____
//                      .   ' \\| |// `.
//                       / \\||| : |||// \
//                     / _||||| -:- |||||- \
//                       | | \\\ - /// | |
//                     | \_| ''\---/'' | |
//                      \ .-\__ `-` ___/-. /
//                   ___`. .' /--.--\ `. . __
//                ."" '< `.___\_<|>_/___.' >'"".
//               | | : `- \`.;`\ _ /`;.`/ - ` : | |
//                 \ \ `-. \_ __\ /__ _/ .-` / /
//         ======`-.____`-.___\_____/___.-`____.-'======
//                            `=---='

// .............................................
// 佛祖镇楼 BUG辟易
// 佛曰:
// 写字楼里写字间，写字间里程序员；
// 程序人员写程序，又拿程序换酒钱。
// 酒醒只在网上坐，酒醉还来网下眠；
// 酒醉酒醒日复日，网上网下年复年。
// 但愿老死电脑间，不愿鞠躬老板前；
// 奔驰宝马贵者趣，公交自行程序员。
// 别人笑我忒疯癫，我笑自己命太贱；
// 不见满街漂亮妹，哪个归得程序员？


