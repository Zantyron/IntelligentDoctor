db = db.getSiblingDB("doctor");

db.chat_session.createIndex({ sessionId: 1 }, { unique: true });
db.chat_session.createIndex({ hospitalId: 1, updatedAt: -1 });
db.chat_message.createIndex({ sessionId: 1, createdAt: 1 });
db.prompt_trace.createIndex({ sessionId: 1, createdAt: -1 });
db.tool_trace.createIndex({ sessionId: 1, createdAt: -1 });
