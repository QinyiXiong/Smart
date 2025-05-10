#!/usr/bin/env node
console.log("Hello World!");

import {McpServer} from "@modelcontextprotocol/sdk/server/mcp.js";
import {StdioServerTransport} from "@modelcontextprotocol/sdk/server/stdio.js";
import mysql from 'mysql2/promise';
import { createClient } from 'redis';
import dotenv from 'dotenv';
import { MongoClient } from 'mongodb';

import {z} from "zod";

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
  console.log('MySQL连接池已创建');
}

// MongoDB初始化
async function initMongoDB() {
  mongoClient = new MongoClient(
    `mongodb://${process.env.MONGO_USERNAME}:${process.env.MONGO_PASSWORD}@${process.env.MONGO_HOST}:${process.env.MONGO_PORT}/${process.env.MONGO_DATABASE}?authSource=${process.env.MONGO_AUTH_DATABASE}`
  );
  await mongoClient.connect();
  console.log('MongoDB已连接');
}

// Redis初始化
async function initRedis() {
  redisClient = createClient({
    url: `redis://${process.env.REDIS_HOST}:${process.env.REDIS_PORT}`,
    password: process.env.REDIS_PASSWORD
  });
  redisClient.on('error', (err) => console.error('Redis Client Error', err));
  await redisClient.connect();
  console.log('Redis已连接');
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
  console.log('所有数据库连接已关闭');
}

// 创建MCP服务器
const server = new McpServer({
    name: "mcp-server",
    version: "1.0.3"
});

server.tool(
    "query_problem",
    "记录用户对应chatId的面试官在某次话题（面试）中的得分",
    {
        difficulty: z.string().optional().describe("当前面试官的对应id，当前用户的Id，当前话题的id"),
        tag: z.string().optional().describe("要查询题目的标签，包含问题所涉及的知识点"),
        keyword: z.string().optional().describe("要查询题目的关键词，会在题目名称，描述中进行匹配搜索"),
    },
    async ({difficulty = "", tag = "", keyword = ""}) => {
        try {
            const mysqlPool = getMySQL();
            
            // 构建SQL查询条件和参数
            let conditions = [];
            let params = [];
            
            // 添加难度条件
            if (difficulty) {
                conditions.push("difficulty = ?");
                params.push(difficulty);
            }
            
            // 添加标签条件
            if (tag) {
                conditions.push("tags LIKE ?");
                params.push(`%${tag}%`);
            }
            
            // 添加关键词条件
            if (keyword) {
                conditions.push("(title LIKE ? OR description LIKE ?)");
                params.push(`%${keyword}%`);
                params.push(`%${keyword}%`);
            }
            
            // 构建完整SQL
            let sql = "SELECT id, problem_code FROM oj_problem";
            if (conditions.length > 0) {
                sql += " WHERE " + conditions.join(" AND ");
            }
            sql += " LIMIT 10"; // 限制返回10条结果
            
            // 执行查询
            const [rows] = await mysqlPool.query(sql, params);
            
            // 格式化结果
            if (rows.length === 0) {
                return {
                    content: [{
                        type: "text",
                        text: "没有找到符合条件的题目"
                    }]
                };
            }
            
            let result = "找到以下题目:\n\n";
            rows.forEach(row => {
                result += `ID: ${row.id} - 题目编号: ${row.problem_code}\n`;
            });
            
            return {
                content: [{type: "text", text: result.trim()}]
            };
            
        } catch (error) {
            console.error("查询题目时出错:", error);
            return {
                content: [{type: "text", text: `查询题目时出错: ${error.message}`}],
                isError: true
            };
        }
    }
);

server.tool(
    "update_valuation",
    {
        chatId: z.string().optional().describe("聊天会话ID"),
        delta: z.number().optional().describe("评分变化值，正数表示加分，负数表示减分"),
        valuationName: z.string().optional().describe("评分项名称")
    },
    async ({chatId, delta = 0, valuationName = ""}) => {
        try {
            const db = getMongoDB();
            const collection = db.collection("valuation_record");
            
            // 查找或创建评分记录
            const result = await collection.findOneAndUpdate(
                { chatId },
                {
                    $setOnInsert: { 
                        chatId,
                        valuationRanks: [] 
                    }
                },
                { 
                    upsert: true,
                    returnDocument: 'after'
                }
            );
            
            // 更新评分
            const updatedRecord = await collection.findOneAndUpdate(
                { 
                    chatId,
                    "valuationRanks.valuation.valuationName": valuationName
                },
                {
                    $inc: { "valuationRanks.$.rank": delta }
                },
                { returnDocument: 'after' }
            );
            
            if (!updatedRecord.value) {
                // 如果评分项不存在，则添加新项
                const valuation = await getMySQL().query(
                    "SELECT * FROM interview_valuation WHERE valuation_name = ?",
                    [valuationName]
                );
                
                if (valuation.length === 0) {
                    return {
                        content: [{type: "text", text: `找不到评分项: ${valuationName}`}],
                        isError: true
                    };
                }
                
                await collection.updateOne(
                    { chatId },
                    {
                        $push: {
                            valuationRanks: {
                                valuation: valuation[0],
                                rank: delta
                            }
                        }
                    }
                );
            }
            
            return {
                content: [{type: "text", text: `成功更新评分: ${valuationName} (${delta > 0 ? '+' : ''}${delta})`}]
            };
            
        } catch (error) {
            console.error("更新评分时出错:", error);
            return {
                content: [{type: "text", text: `更新评分时出错: ${error.message}`}],
                isError: true
            };
        }
    }
);
// 启动服务器
async function main() {
    try {
        console.log("启动模拟面试的MCP服务器...");
        await initMySQL();
        await initMongoDB();
        await initRedis();
        
        const transport = new StdioServerTransport();
        await server.connect(transport);

        console.log("MCP服务器已经开始等待连接");
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

