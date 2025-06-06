# selenium-on-awslambda
JavaでSelenium開発したプログラムをAWS Lambdaで動作させるサンプルプログラム

![Java](https://img.shields.io/badge/Java:21-Langage-007396?logo=openjdk&logoColor=white)
![Selenium](https://img.shields.io/badge/Selenium-Automation-43B02A?logo=selenium&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-Build%20Tool-C71A36?logo=apache-maven&logoColor=white)
![AWS Lambda](https://img.shields.io/badge/AWS%20Lambda-Serverless-FF9900?logo=aws-lambda&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED?logo=docker&logoColor=white)
![Debian](https://img.shields.io/badge/Debian-Linux%20OS-A81D33?logo=debian&logoColor=white)

## 公開の目的
Java Seleniumで開発したWebSiteの自動処理プログラムをAWS Lambdaで動作させるコードの共有が目的です。  
Seleniumの動作に必要な最低限のOptionのみを指定しています。  
SeleniumプログラムをAWS Lambdaで動かすための多くの情報が溢れているのですが、私が参考にした際に色々と混乱した点があったため、このコードで整理し情報を共有したいと思ったのが公開の背景です。  
Browser、DriverはAWS Lambda用にbuildしたものでなくても、リポジトリ公式パッケージで動作するので、ぜひ、ソフトウェア技術者の皆さんは参考にしてください。

## このサンプルプログラムの概要
AWS Lambdaで動くSeleniumプログラムのサンプルとして[官報](https://www.kanpo.go.jp/)で公開されている「本日の官報」のPDFをダウンロードするプログラムを作っています。tmpフォルダに作ったサブフォルダにPDFファイルがダウンロードされます。<br/>
※ダウンロードしたPDFに対する処理は作っていないです。

[KanpoWebOperationApp](src%2Fmain%2Fjava%2Fgreenflagproject%2Fselenium%2Fapps%2FKanpoWebOperationApp.java) Classが官報のWebサイトに対する処理です。継承元Classの[AutoWebOperator](src%2Fmain%2Fjava%2Fgreenflagproject%2Fselenium%2Fcommon%2FAutoWebOperator.java)Classを通じてRemoteDriverを操作する処理が実行できます。<br/>
AWS Lambdaのhandler(通常、CMDで指定するClassとmethod)は、[AwsLambdaRequestHandler](src%2Fmain%2Fjava%2Fgreenflagproject%2Fselenium%2Fapps%2FAwsLambdaRequestHandler.java)::handleRequestです。

## 実行方法
docker builして、docker runしてください。
```
# ビルド
docker build --provenance=false --platform=linux/arm64 -t autoweb:test-0.0.1 .

# Docker実行
docker run --rm --platform linux/arm64 -p 9000:8080 -it autoweb:test-0.0.1

# Docker実行したコンテナのLambdaを動かすイベントを発行(このイベントによってAwsLambdaRequestHandler::handleRequestが呼び出され、KanpoWebOperationAppを動かします。)
curl "http://localhost:9000/2015-03-31/functions/function/invocations" -d '{}'
```
AWS Lambdaに載せる方法はAWSのドキュメント[コンテナイメージを使用した Java Lambda 関数のデプロイ](https://docs.aws.amazon.com/ja_jp/lambda/latest/dg/java-image.html)を参照してください。

## このプログラムをベースに開発する際の手引き
[AutoWebOperator](src%2Fmain%2Fjava%2Fgreenflagproject%2Fselenium%2Fcommon%2FAutoWebOperator.java)をextendsしたClassを作成し、そのクラスにWebSiteに対する自動処理を記載してください。  
AutoWebOperator Classを通じてSeleniumWebDriverにアクセスできます。
デフォルトでChrome(Chromium)を使うように作っていますが、環境変数を設定すればFirefoxでも動作するような仕掛けを入れています。

## License
このプログラムは[MIT license](https://en.wikipedia.org/wiki/MIT_License)です。
ロジック的なことは何もしていないし...

