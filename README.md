# playersync-protocol

跨端共享的协议词汇库，由 PlayerSync Forge mod 与 PlayerSyncTransfer Velocity 插件共同依赖。

- Group: `dev.kavinshi`
- Version: `0.1.0`
- Java: 17
- 依赖: 无第三方依赖（仅 JDK）

## 构建

```
./gradlew build
```

## 范围

仅放置跨端共享的稳定值对象、枚举与纯函数。禁止放入 Forge/Velocity/Minecraft/JDBC 特有类型。
