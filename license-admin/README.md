# license-admin

独立授权项目，只负责：

- 生成 RSA 密钥对
- 录入客户信息
- 勾选授权功能
- 生成 `.lic` 授权文件

默认已经内置了一份与当前原项目公钥匹配的签发私钥，所以直接生成的授权文件可以被当前原项目验证通过。

如果你改用自己新生成的密钥对：

1. 用本项目生成新的 `public-key.pem`
2. 替换原项目里的 [public-key.pem](/F:/public-Xiangqi/src/main/resources/license/public-key.pem)
3. 再用新的私钥签发授权

## 启动

```powershell
mvn javafx:run
```

## 打包

```powershell
mvn -DskipTests package
```
