FROM ubuntu:20.04

# 避免交互式提示
ARG DEBIAN_FRONTEND=noninteractive

# 设置时区
RUN apt-get update && apt-get install -y tzdata
RUN ln -fs /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && dpkg-reconfigure -f noninteractive tzdata

# 安装基础工具
RUN apt-get update && apt-get install -y \
    build-essential \
    gcc \
    g++ \
    openjdk-11-jdk \
    python3 \
    python3-pip \
    curl \
    wget \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# 创建工作目录
RUN mkdir -p /judge
WORKDIR /judge

# 设置Java和Python别名
RUN ln -sf /usr/bin/python3 /usr/bin/python
RUN ln -sf /usr/bin/java /usr/bin/java11

# 设置环境变量
ENV PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64

# 创建非root用户运行代码
RUN useradd -m -u 1000 judge
RUN chown -R judge:judge /judge

# 设置默认命令
CMD ["bash"]