FROM debian:bookworm-slim
### https://hub.docker.com/_/debian

### パッケージインストール updateしないとgnupdなどがインストールできない
COPY docker/apt.bookworm.snapshot.list /etc/apt/sources.list.d/snapshot.list
RUN apt update && \
    apt install -y gnupg curl

### Amazon Corretto 21 Installation Instructions for Debian-Based, RPM-Based and Alpine Linux Distributions
### https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/generic-linux-install.html
RUN curl -s https://apt.corretto.aws/corretto.key | gpg --dearmor -o /usr/share/keyrings/corretto-keyring.gpg && \
    echo "deb [signed-by=/usr/share/keyrings/corretto-keyring.gpg] https://apt.corretto.aws stable main" | tee /etc/apt/sources.list.d/corretto.list
RUN apt update; apt install -y java-21-amazon-corretto-jdk

### Install Chromium
### https://www.chromium.org/chromium-projects/
ARG CHROMIUM_VERSION=137.0.7151.55-3~deb12u1
RUN apt install -y chromium-driver=${CHROMIUM_VERSION} chromium-common=${CHROMIUM_VERSION} chromium=${CHROMIUM_VERSION}

### AWS Lambda 用カスタムランタイムの構築
### https://docs.aws.amazon.com/ja_jp/lambda/latest/dg/runtimes-custom.html
RUN mkdir -p /var/runtime
ENV LAMBDA_RUNTIME_DIR="/var/runtime"
ENV LAMBDA_TASK_ROOT=/var/task
WORKDIR ${LAMBDA_TASK_ROOT}

### ローカルでAWS Lambdaをシミュレーションしてテストするための対応
### https://docs.aws.amazon.com/ja_jp/lambda/latest/dg/java-image.html
RUN mkdir -p ~/.aws-lambda-rie && \
    curl -Lo /usr/local/bin/aws-lambda-rie https://github.com/aws/aws-lambda-runtime-interface-emulator/releases/latest/download/aws-lambda-rie-arm64 && \
    chmod +x /usr/local/bin/aws-lambda-rie && \
    rm -r ~/.aws-lambda-rie
RUN chmod +x /usr/local/bin/aws-lambda-rie

COPY target/classes ${LAMBDA_TASK_ROOT}
COPY target/dependency/* ${LAMBDA_TASK_ROOT}/lib/

### 日本語
RUN apt install -y locales &&\
    echo "ja_JP.UTF-8 UTF-8" > /etc/locale.gen &&\
    locale-gen ja_JP.UTF-8

RUN apt clean && rm -rf /var/lib/apt/lists/*


ENV HOSTNAME=weboperator

#AWS Lambda環境変数の設定
ENV LANG=ja_JP.UTF-8
ENV WEBDRIVER_CHROME_BINARY=/usr/bin/chromium
ENV WEBDRIVER_CHROME_DRIVER=/usr/bin/chromedriver
# selenium-managerは動かないようにし、cacheはAWS実行時writeできる/tmpに作るようにした
ENV SE_DISABLE_SELENIUM_MANAGER=true
ENV SE_CACHE_PATH=/tmp
ENV HOME=/tmp

COPY docker/lambda-entrypoint.sh /
RUN chmod +x /lambda-entrypoint.sh

# コンテナイメージを使用した Java Lambda 関数のデプロイ
# https://docs.aws.amazon.com/ja_jp/lambda/latest/dg/java-image.html
#ENTRYPOINT [ "/usr/bin/java", "-cp", "./:./lib/*", "com.amazonaws.services.lambda.runtime.api.client.AWSLambda" ]
ENTRYPOINT [ "/lambda-entrypoint.sh" ]
CMD [ "greenflagproject.selenium.apps.AwsLambdaRequestHandler::handleRequest" ]

# Docker commands example
# docker build --provenance=false --platform=linux/arm64 -t ecr/autoweb:test-0.0.1 .
# docker run --rm --platform linux/arm64 -p 9000:8080 -it ecr/autoweb:test-0.0.1

# ローカルテスト(Java App直接実行)
# docker run --rm --entrypoint '' --platform linux/arm64  -p 9000:8080 -it ecrß/autoweb:test-0.0.1 /bin/bash
# java -cp "./:./lib/*" greenflagproject.selenium.apps.KanpoWebOperationApp
# chromium --headless --no-sandbox  --disable-gpu --dump-dom https://www.google.com

# ローカルテスト(AWS Lambdaシミュレーション)
# aws-lambda-rie /usr/bin/java -cp './:./lib/*' com.amazonaws.services.lambda.runtime.api.client.AWSLambda greenflagproject.selenium.apps.AwsLambdaRequestHandler::handleRequest
# curl "http://localhost:9000/2015-03-31/functions/function/invocations" -d '{}'

# docker run --platform linux/amd64 -d -v ~/.aws-lambda-rie:/aws-lambda -p 9000:8080 \
#    --entrypoint /aws-lambda/aws-lambda-rie \
#    docker-image:test \
#        /usr/bin/java -cp './:./lib/*' com.amazonaws.services.lambda.runtime.api.client.AWSLambda greenflagproject.selenium.apps.KanpoWebOperationApp::handleRequest

# お役立ち
# docker system prune -f
# -Djava.net.preferIPv4Stack=true -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG
