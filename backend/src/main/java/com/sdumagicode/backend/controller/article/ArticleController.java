package com.sdumagicode.backend.controller.article;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.sdumagicode.backend.core.exception.BusinessException;
import com.sdumagicode.backend.core.exception.ServiceException;
import com.sdumagicode.backend.core.result.GlobalResult;
import com.sdumagicode.backend.core.result.GlobalResultGenerator;
import com.sdumagicode.backend.core.service.security.annotation.AuthorshipInterceptor;
import com.sdumagicode.backend.dto.ArticleDTO;
import com.sdumagicode.backend.dto.CommentDTO;
import com.sdumagicode.backend.entity.*;
import com.sdumagicode.backend.enumerate.Module;
import com.sdumagicode.backend.service.*;
import com.sdumagicode.backend.util.UserUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Objects;

/**
 * @author ronger
 */
@RestController
@RequestMapping("/api/v1/article")
public class ArticleController {

    @Resource
    private ArticleService articleService;
    @Resource
    private CommentService commentService;
    @Resource
    private ArticleThumbsUpService articleThumbsUpService;
    @Resource
    private SponsorService sponsorService;

    @Autowired
    ChatService chatService;

    @Autowired
    InterviewerService interviewerService;

    @GetMapping("/detail/{idArticle}")
    public GlobalResult<ArticleDTO> detail(@PathVariable Long idArticle, @RequestParam(defaultValue = "2") Integer type) {
        ArticleDTO dto = articleService.findArticleDTOById(idArticle, type);
        return GlobalResultGenerator.genSuccessResult(dto);
    }

    @PostMapping("/post")
    @RequiresPermissions(value = "user")
    public GlobalResult<Long> postArticle(@RequestBody ArticleDTO article) throws UnsupportedEncodingException {
        User user = UserUtils.getCurrentUserByToken();
        return GlobalResultGenerator.genSuccessResult(articleService.postArticle(article, user));
    }

    @PutMapping("/post")
    @AuthorshipInterceptor(moduleName = Module.ARTICLE)
    public GlobalResult<Long> updateArticle(@RequestBody ArticleDTO article) throws UnsupportedEncodingException {
        User user = UserUtils.getCurrentUserByToken();
        return GlobalResultGenerator.genSuccessResult(articleService.postArticle(article, user));
    }

    @DeleteMapping("/delete/{idArticle}")
    @AuthorshipInterceptor(moduleName = Module.ARTICLE)
    public GlobalResult<Integer> delete(@PathVariable Long idArticle) {
        return GlobalResultGenerator.genSuccessResult(articleService.delete(idArticle));
    }

    @GetMapping("/{idArticle}/comments")
    public GlobalResult<PageInfo<CommentDTO>> commons(@PathVariable Integer idArticle, @RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer rows) {
        PageHelper.startPage(page, rows);
        List<CommentDTO> list = commentService.getArticleComments(idArticle);
        PageInfo<CommentDTO> pageInfo = new PageInfo<>(list);
        return GlobalResultGenerator.genSuccessResult(pageInfo);
    }

    @GetMapping("/drafts")
    @RequiresPermissions(value = "user")
    public GlobalResult<PageInfo<ArticleDTO>> drafts(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "10") Integer rows) {
        PageHelper.startPage(page, rows);
        User user = UserUtils.getCurrentUserByToken();
        List<ArticleDTO> list = articleService.findDrafts(user.getIdUser());
        PageInfo<ArticleDTO> pageInfo = new PageInfo<>(list);
        return GlobalResultGenerator.genSuccessResult(pageInfo);
    }

    @GetMapping("/{idArticle}/share")
    @RequiresPermissions(value = "user")
    public GlobalResult<String> share(@PathVariable Integer idArticle) {
        User user = UserUtils.getCurrentUserByToken();
        return GlobalResultGenerator.genResult(true, articleService.share(idArticle, user.getAccount()), "");
    }

    @PostMapping("/update-tags")
    @RequiresPermissions(value = "user")
    @AuthorshipInterceptor(moduleName = Module.ARTICLE_TAG)
    public GlobalResult<Boolean> updateTags(@RequestBody Article article) throws UnsupportedEncodingException {
        Long idArticle = article.getIdArticle();
        String articleTags = article.getArticleTags();
        User user = UserUtils.getCurrentUserByToken();
        return GlobalResultGenerator.genSuccessResult(articleService.updateTags(idArticle, articleTags, user.getIdUser()));
    }

    @PostMapping("/thumbs-up")
    @RequiresPermissions(value = "user")
    public GlobalResult<Integer> thumbsUp(@RequestBody ArticleThumbsUp articleThumbsUp) {
        if (Objects.isNull(articleThumbsUp) || Objects.isNull(articleThumbsUp.getIdArticle())) {
            throw new BusinessException("数据异常,文章不存在!");
        }
        User user = UserUtils.getCurrentUserByToken();
        articleThumbsUp.setIdUser(user.getIdUser());
        return GlobalResultGenerator.genSuccessResult(articleThumbsUpService.thumbsUp(articleThumbsUp));
    }

    @PostMapping("/sponsor")
    @RequiresPermissions(value = "user")
    public GlobalResult<Boolean> sponsor(@RequestBody Sponsor sponsor) {
        if (Objects.isNull(sponsor) || Objects.isNull(sponsor.getDataId()) || Objects.isNull(sponsor.getDataType())) {
            throw new IllegalArgumentException("数据异常");
        }
        User user = UserUtils.getCurrentUserByToken();
        sponsor.setSponsor(user.getIdUser());
        Boolean flag = sponsorService.sponsorship(sponsor);
        return GlobalResultGenerator.genSuccessResult(flag);
    }

    @PostMapping("/referenceShare")
    public GlobalResult referenceShare(@RequestBody ShareReference shareReference){
        Integer type = shareReference.getType();
        Long idUser = UserUtils.getCurrentUserByToken().getIdUser();
        switch (type){
            case 0:{
                chatService.deepCopy(shareReference.getChatId(),idUser);
                break;
            }
            case 1:{
                interviewerService.deepCopy(shareReference.getInterviewerId(),idUser);
                break;
            }
            default:
                throw new ServiceException("分享异常");
        }

        return GlobalResultGenerator.genSuccessResult("获取分享内容成功");
    }

    @PostMapping("/{articleId}/interview/{interviewId}/get")
    @RequiresPermissions(value = "user")
    public GlobalResult getInterview(@PathVariable Long articleId, @PathVariable Long interviewId) {
        Long idUser = UserUtils.getCurrentUserByToken().getIdUser();
        
        chatService.deepCopy(interviewId, idUser);
        return GlobalResultGenerator.genSuccessResult("获取面试记录成功");
    }

}
