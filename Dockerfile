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
RUN apt install -y chromium-headless-shell=${CHROMIUM_VERSION} chromium-driver=${CHROMIUM_VERSION} chromium-common=${CHROMIUM_VERSION} chromium=${CHROMIUM_VERSION}

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
# RUN apt install -y task-japanese locales-all

### xvfb:ディスプレイ、印刷:cups
RUN apt install -y xvfb cups dbus-x11 dbus-user-session
### その他依存関係
RUN apt install -y libasound2-dev libdbus-glib-1-dev libgtk2.0-0 libxss1 libgbm-dev \
      ca-certificates \
      fonts-liberation \
      fonts-noto-color-emoji \
      libasound2 \
      libatk-bridge2.0-0 \
      libatk1.0-0 \
      libc6 \
      libcairo2 \
      libcups2 \
      libdbus-1-3 \
      libdrm2 \
      libexpat1 \
      libgbm1 \
      libgcc-s1 \
      libglib2.0-0 \
      libgtk-3-0 \
      libnspr4 \
      libnss3 \
      libpango-1.0-0 \
      libpangocairo-1.0-0 \
      libstdc++6 \
      libx11-6 \
      libx11-xcb1 \
      libxcb1 \
      libxcomposite1 \
      libxcursor1 \
      libxdamage1 \
      libxext6 \
      libxfixes3 \
      libxi6 \
      libxrandr2 \
      libxrender1 \
      libxtst6 \
      lsb-release \
      xdg-utils \
     --no-install-recommends

RUN apt clean && rm -rf /var/lib/apt/lists/*


ENV HOSTNAME=weboperator

#AWS Lambda環境変数の設定
ENV LANG=ja_JP.UTF-8
ENV DBUS_SESSION_BUS_ADDRESS=/dev/null
#ENV DBUS_SESSION_BUS_ADDRESS=disabled
ENV WDM_CACHEPATH=/tmp
ENV HOME=/tmp
ENV WEBDRIVER_CHROME_BINARY=/usr/bin/chromium
ENV WEBDRIVER_CHROME_DRIVER=/usr/bin/chromedriver
ENV EXECUTE_MAX_RETRY_TIMES=1
# Selenium-managerの設定
# https://www.selenium.dev/documentation/selenium_manager/
# /tmpだとAWS上ではプログラムが実行できない(/var/runtimeはLAMBDA_RUNTIME_DIRの値)
# /var/runtimeだと書き込めない
# selenium-managerは動かないようにし、cacheは/tmpに作るようにした
ENV SE_CACHE_PATH=/tmp

# Selenium-managerを使わないようにする(true:使わない→selenium managerを無効にするの意味)
ENV SE_DISABLE_SELENIUM_MANAGER=true

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
