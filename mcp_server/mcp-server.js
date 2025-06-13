#!/usr/bin/env node


import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import mysql from 'mysql2/promise';
import { createClient } from 'redis';
import dotenv from 'dotenv';
import { MongoClient } from 'mongodb';
import axios from 'axios'; // 引入axios

import { z } from "zod";

dotenv.config({ override: true });

// 数据库连接部分
let mysqlPool;
let mongoClient;
let redisClient;

// MySQL初始化
async function initMySQL() {
    mysqlPool = mysql.createPool({
        host: process.env.MYSQL_HOST,
        port: process.env.MYSQL_PORT,
        user: process.env.MYSQL_USER,
        password: process.env.MYSQL_PASSWORD,
        database: 'forest',
        waitForConnections: true,
        connectionLimit: 10,
        queueLimit: 0
    });
   
}

// MongoDB初始化
async function initMongoDB() {
    mongoClient = new MongoClient(
        `mongodb://${process.env.MONGO_USERNAME}:${process.env.MONGO_PASSWORD}@${process.env.MONGO_HOST}:${process.env.MONGO_PORT}/${process.env.MONGO_DATABASE}?authSource=${process.env.MONGO_AUTH_DATABASE}`
    );
    await mongoClient.connect();
  
}

// Redis初始化
async function initRedis() {
    redisClient = createClient({
        url: `redis://${process.env.REDIS_HOST}:${process.env.REDIS_PORT}`,
        password: process.env.REDIS_PASSWORD
    });
    redisClient.on('error', (err) => console.error('Redis Client Error', err));
    await redisClient.connect();
   
}

// 获取数据库连接
function getMySQL() {
    if (!mysqlPool) throw new Error('MySQL连接池未初始化');
    return mysqlPool;
}

function getMongoDB() {
    if (!mongoClient) throw new Error('MongoDB客户端未初始化');
    return mongoClient.db(process.env.MONGO_DATABASE);
}

function getRedis() {
    if (!redisClient) throw new Error('Redis客户端未初始化');
    return redisClient;
}

// 关闭所有数据库连接
async function closeAll() {
    if (mysqlPool) await mysqlPool.end();
    if (redisClient) await redisClient.quit();
    if (mongoClient) await mongoClient.close();
    
}

// 创建MCP服务器
const server = new McpServer({
    name: "mcp-server",
    version: "1.0.3"
});

server.tool(
    "y",
    "获取合适的题目信息",
    {
        prompt: z.string().describe("描述你想查找的题目,如难度,标签,描述等"),
        chatId: z.string().describe("聊天会话ID,为整形数字的字符串格式")
    },
    async ({ prompt = "", chatId = "" }) => {
        // 若没有配置环境变量，可用百炼API Key将下行替换为：apiKey='sk-xxx'。但不建议在生产环境中直接将API Key硬编码到代码中，以减少API Key泄露风险。
        const apiKey = process.env.DASHSCOPE_API_KEY;
        const appId = process.env.DASHSCOPE_APP_ID; // 从环境变量读取APP_ID

        if (!apiKey || !appId) {
            console.error('DashScope API Key or App ID not configured.');
            return {
                content: [{ type: "text", text: "DashScope API Key or App ID not configured." }],
                isError: true
            };
        }

        const url = `https://dashscope.aliyuncs.com/api/v1/apps/${appId}/completion`;

        const data = {
            input: {
                prompt: prompt // 使用传入的prompt
            },
            parameters: {},
            debug: {}
        };

        try {
            const response = await axios.post(url, data, {
                headers: {
                    'Authorization': `Bearer ${apiKey}`,
                    'Content-Type': 'application/json'
                }
            });

            if (response.status === 200 && response.data.output && response.data.output.text) {
                const text = response.data.output.text;
                // 解析text格式为ID:XX - 题目编号:XX ,例如:"ID: 12 - 题目编号: P1022"
                const idMatch = text.match(/ID:\s*(\S+)/);
                const codeMatch = text.match(/题目编号:\s*(\S+)/);

                if (idMatch && idMatch[1] && codeMatch && codeMatch[1]) {
                    const problemId = idMatch[1];
                    const problemCode = codeMatch[1];


                    try {
                        const redis = getRedis();
                        const actionKey = `action${chatId}_push`;
                        const actionData = {
                            action: "push",
                            chatId: chatId, // 使用解析或获取到的chatId
                            problemId: problemId,
                            code: problemCode
                        };

                        await redis.select(1); // 根据用户描述，应该是db1
                        await redis.set(actionKey, JSON.stringify(actionData));
                        await redis.expire(actionKey, 3 * 60);
                        console.log(`Action data saved to Redis: key=${actionKey}`);
                    } catch (redisError) {
                        console.error(`Error saving to Redis: ${redisError.message}`);
                    }
                }

                return {
                    content: [{ type: "text", text: text }]
                };
            } else {
                console.log(`request_id=${response.headers['request_id']}`);
                console.log(`code=${response.status}`);
                console.log(`message=${response.data.message}`);
                return {
                    content: [{ type: "text", text: `Error from DashScope: ${response.data.message || response.status}` }],
                    isError: true
                };
            }
        } catch (error) {
            console.error(`Error calling DashScope: ${error.message}`);
            if (error.response) {
                console.error(`Response status: ${error.response.status}`);
                console.error(`Response data: ${JSON.stringify(error.response.data, null, 2)}`);
            }
            return {
                content: [{ type: "text", text: `Error calling DashScope: ${error.message}` }],
                isError: true
            };
        }
    }
);


// server.tool(
//     "generate_problem_selection_prompt",
//     "根据面试表现和简历信息生成算法题选择提示",
//     {
//         chatId: z.string().describe("聊天会话ID,为整形数字的字符串格式"),
//         assessmentText: z.string().describe("面试评估文本，包含需要考察的算法/数据结构关键词"),
//         resumeText: z.string().optional().describe("简历文本，用于提取候选人技能背景，可选"),
//         difficultyPreference: z.string().optional().describe("难度偏好，如'简单'，'中等'，'困难'或它们的组合，可选")
//     },
//     async ({ chatId, assessmentText, resumeText = "", difficultyPreference = "中等" }) => {
//         try {
//             // 1. 提取需要考察的标签/关键词
//             const requiredTags = await calculateRequiredTags(resumeText, assessmentText);
//
//             // 2. 确定难度分布
//             let difficultyProfile = difficultyPreference;
//
//             // 3. 如果有历史记录，可以调整难度
//             try {
//                 const db = getMongoDB();
//                 const chatIdInt = parseInt(chatId);
//                 if (!isNaN(chatIdInt)) {
//                     const historyRecord = await db.collection("interview_history").findOne({ chatId: chatIdInt });
//                     if (historyRecord) {
//                         difficultyProfile = buildDifficultyProfile(historyRecord) || difficultyProfile;
//                     }
//                 }
//             } catch (dbError) {
//                 console.error(`Error retrieving interview history: ${dbError.message}`);
//                 // 继续使用默认难度配置
//             }
//
//             // 4. 构建查询提示
//             const tagString = requiredTags.join(", ");
//             const queryPrompt = `请为我查找一道${difficultyProfile}难度的算法题，要求包含以下标签或主题中的至少一个：${tagString}。
//             题目应该具有清晰的描述、合理的约束条件，并且适合在面试中讨论解题思路。
//             请返回题目的ID和题目编号，格式为"ID: XX - 题目编号: XX"。`;
//
//             console.log(`Generated problem selection prompt: ${queryPrompt}`);
//
//             // 5. 调用DashScope API查询题目
//             const apiKey = process.env.DASHSCOPE_API_KEY;
//             const appId = process.env.DASHSCOPE_APP_ID;
//
//             if (!apiKey || !appId) {
//                 console.error('DashScope API Key or App ID not configured.');
//                 return {
//                     content: [{ type: "text", text: "DashScope API Key or App ID not configured." }],
//                     isError: true
//                 };
//             }
//
//             const url = `https://dashscope.aliyuncs.com/api/v1/apps/${appId}/completion`;
//
//             const data = {
//                 input: {
//                     prompt: queryPrompt
//                 },
//                 parameters: {},
//                 debug: {}
//             };
//
//             const response = await axios.post(url, data, {
//                 headers: {
//                     'Authorization': `Bearer ${apiKey}`,
//                     'Content-Type': 'application/json'
//                 }
//             });
//
//             if (response.status === 200 && response.data.output && response.data.output.text) {
//                 const text = response.data.output.text;
//                 // 解析text格式为ID:XX - 题目编号:XX ,例如:"ID: 12 - 题目编号: P1022"
//                 const idMatch = text.match(/ID:\s*(\S+)/);
//                 const codeMatch = text.match(/题目编号:\s*(\S+)/);
//
//                 if (idMatch && idMatch[1] && codeMatch && codeMatch[1]) {
//                     const problemId = idMatch[1];
//                     const problemCode = codeMatch[1];
//
//                     try {
//                         const redis = getRedis();
//                         const actionKey = `action${chatId}_push`;
//                         const actionData = {
//                             action: "push",
//                             chatId: chatId,
//                             problemId: problemId,
//                             code: problemCode
//                         };
//
//                         await redis.select(1);
//                         await redis.set(actionKey, JSON.stringify(actionData));
//                         await redis.expire(actionKey, 3 * 60);
//                         console.log(`Action data saved to Redis: key=${actionKey}`);
//                     } catch (redisError) {
//                         console.error(`Error saving to Redis: ${redisError.message}`);
//                     }
//                 }
//
//                 // 6. 记录本次查询到MongoDB（可选）
//                 try {
//                     const db = getMongoDB();
//                     const chatIdInt = parseInt(chatId);
//                     if (!isNaN(chatIdInt)) {
//                         await db.collection("problem_selections").insertOne({
//                             chatId: chatIdInt,
//                             timestamp: new Date(),
//                             requiredTags,
//                             difficultyProfile,
//                             selectedProblem: {
//                                 id: idMatch && idMatch[1] ? idMatch[1] : null,
//                                 code: codeMatch && codeMatch[1] ? codeMatch[1] : null
//                             },
//                             fullResponse: text
//                         });
//                     }
//                 } catch (dbError) {
//                     console.error(`Error logging problem selection: ${dbError.message}`);
//                     // 非关键错误，继续执行
//                 }
//
//                 return {
//                     content: [{ type: "text", text: text }]
//                 };
//             } else {
//                 console.log(`request_id=${response.headers['request_id']}`);
//                 console.log(`code=${response.status}`);
//                 console.log(`message=${response.data.message}`);
//                 return {
//                     content: [{ type: "text", text: `Error from DashScope: ${response.data.message || response.status}` }],
//                     isError: true
//                 };
//             }
//         } catch (error) {
//             console.error(`Error in generate_problem_selection_prompt: ${error.message}`);
//             if (error.response) {
//                 console.error(`Response status: ${error.response.status}`);
//                 console.error(`Response data: ${JSON.stringify(error.response.data, null, 2)}`);
//             }
//             return {
//                 content: [{ type: "text", text: `Error generating problem selection prompt: ${error.message}` }],
//                 isError: true
//             };
//         }
//     }
// );

// 辅助函数示例
async function calculateRequiredTags(resumeText, assessmentText) {
    // 实现标签匹配逻辑（示例）
    const tags = [];
    if (resumeText.includes("数组")) tags.push("数组");
    if (assessmentText.includes("动态规划")) tags.push("动态规划");
    return tags.length > 0 ? tags : ["二分查找", "哈希表"];
}

function buildDifficultyProfile(record) {
    // 根据历史记录生成难度分布（示例）
    return record?.difficultyHistory
        ? `${record.difficultyHistory.join("/")} 递进`
        : "中等为主，包含20%困难题";
}

server.tool(
    "update_valuation",
    "更新用户在对应chatId的面试中的某方面的得分",
    {
        chatId: z.string().describe("聊天会话ID,为整形数字的字符串格式"),
        delta: z.number().optional().describe("评分变化值，正数表示加分，负数表示减分"),
        valuationName: z.string().describe("评分项名称,有项目经验,基础知识,算法能力,系统设计,沟通能力五个模块")
    },
    async ({ chatId, delta = 0, valuationName = "" }) => {
        try { // <--- 添加 try 块
            const db = getMongoDB();
            const collection = db.collection("valuation_record");
            // 将chatId转换为整数形式
            const chatIdInt = parseInt(chatId);
            if (isNaN(chatIdInt)) {
                return {
                    content: [{ type: "text", text: `无效的chatId格式: ${chatId}` }],
                    isError: true
                };
            }

            // 查找或创建评分记录
            const result = await collection.findOne({ "chatId": chatIdInt });
            if (!result) {
                return {
                    content: [{ type: "text", text: `找不到聊天会话记录: ${chatIdInt}` }],
                    isError: true
                };
            }

            // 更新评分
            await collection.findOneAndUpdate(
                {
                    "chatId": chatIdInt,
                    "valuationRanks.valuation.valuationName": valuationName
                },
                {
                    $inc: { "valuationRanks.$.rank": delta }
                },
                { returnDocument: 'after' }
            );

            // 将操作记录存入Redis
            const redis = getRedis();
            const actionKey = `action${chatIdInt}_${valuationName}`;
            const actionData = {
                action: "update_valuation",
                chatId: chatIdInt,
                valuationName: valuationName,
                delta: delta
            };

            // 将数据存入Redis的db10
            await redis.select(1);
            await redis.set(actionKey, JSON.stringify(actionData));
            // 设置过期时间为3分钟
            await redis.expire(actionKey, 3 * 60);

            return {
                content: [{ type: "text", text: `成功更新评分: ${valuationName} (${delta > 0 ? '+' : ''}${delta})` }]
            };
        } catch (error) { // <--- 添加 catch 块
            console.error(`Error in update_valuation tool for chatId ${chatId}, valuationName ${valuationName}:`, error); // 添加更详细的日志
            return {
                content: [{ type: "text", text: `更新评分时发生内部错误: ${error.message}` }], // 返回捕获到的错误信息
                isError: true
            };
        }
    }
);

server.tool(
    "query_test_result",
    "根据chatId查询面试者的代码测试结果,当面试者发送''用户已返回''时,调用该工具",
    {
        chatId: z.string().describe("聊天会话ID,为整形数字的字符串格式"),
    },
    async ({ chatId }) => {
        try {
            const redis = getRedis();
            await redis.select(1); // 使用db1

            // 获取所有与该chatId相关的测试结果
            const keys = await redis.keys(`problem_results:${chatId}:*`);

            if (!keys || keys.length === 0) {
                return {
                    content: [{ type: "text", text: `未找到chatId为${chatId}的测试结果` }],
                    isError: true
                };
            }

            // 获取所有结果
            const results = [];

            for (const key of keys) {
                // 获取Redis中存储的数据
                const data = await redis.get(key);
                if (!data) continue;

                const parsedData = JSON.parse(data);
                const formattedResult = {};

                // 修改后的判题机结果处理
                if (parsedData.judgeResult && Object.keys(parsedData.judgeResult).length > 0) {
                    formattedResult["判题机结果"] = {
                        "题目难度": parsedData.judgeResult.difficulty,
                        "状态": parsedData.judgeResult.status,
                        "执行时间(ms)": parsedData.judgeResult.time,
                        "内存占用(KB)": parsedData.judgeResult.memory,
                        "通过测试用例数量": parsedData.judgeResult.passedTestCases,
                        "总测试用例数量": parsedData.judgeResult.totalTestCases
                    };
                } else {
                    formattedResult["判题机结果"] = "用户未进行代码检验";
                }

                // 修改后的AI评测结果处理
                if (parsedData.aiReviewResult &&
                    parsedData.aiReviewResult !== '正在分析代码，请稍候...' &&
                    Object.keys(parsedData.aiReviewResult).length > 0) {
                    formattedResult["AI审阅代码结果"] = parsedData.aiReviewResult;
                } else {
                    formattedResult["AI审阅代码结果"] = "用户未进行AI代码评测";
                }

                // 提取branchId
                const branchId = key.split(':')[2];
                formattedResult["分支ID"] = branchId;

                results.push(formattedResult);

                // 从Redis中删除该数据
                await redis.del(key);
            }

            return {
                content: [{ type: "text", text: JSON.stringify(results, null, 2) }]
            };
        } catch (error) {
            console.error(`查询测试结果时发生错误: ${error.message}`);
            return {
                content: [{ type: "text", text: `查询测试结果时发生内部错误: ${error.message}` }],
                isError: true
            };
        }
    }
);


// 启动服务器
async function main() {
    try {
        
        await initMySQL();
        await initMongoDB();
        await initRedis();

        const transport = new StdioServerTransport();
        await server.connect(transport);

        
    } catch (error) {
        console.error("启动服务器时出错:", error);
        process.exit(1);
    } finally {
        process.on('SIGINT', async () => {
            await closeAll();
            process.exit(0);
        });
    }
}

main();

