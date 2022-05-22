# 提瓦特旅游抢票系统

## 简介

两个月之内从Java基础背到Java虚拟机，中间夹杂各种各样的知识，自觉掌握不是很牢固，所以希望通个这个项目巩固所学，夯实基础，作为我在单体架构的最后一个项目，为未来进军分布式做准备。

## 背景

考虑到版权问题，不好直接套用现有抢购网站的静态资源，所以本系统所有静态资源均来自游戏——原神的截图。现提瓦特旅行团想要开设一个旅游网站，用户可以在该网站看到提瓦特大陆优美的风景，若喜欢还可以购票进行旅游。不过由于规模问题，各风景站点票数有限，所以需要用户在规定时间内抢票，并及时下单。系统要求吞吐量、TPS、RTT、有效支付率等指标在一定范围内。

## 快速使用

### Jmeter安装

必须通过 [Jmeter](https://jmeter.apache.org/) 启动本项目，版本最好在 5.4.3 附近

### 简单使用

如果你只想启动项目看看是否能正常运行

> 网页演示

1. 浏览器打开本系统的演示网址（挂掉后就请自行本地搭建）
2. 克隆项目后端的[master版本](https://github.com/coldairance/STServer)到本地。
3. 打开 STServer/test/ 文件夹

```sh
jmeter -n -t Base.jmx
```

你可以尝试访问以下路径

| url            | desc                   |
| -------------- | ---------------------- |
| /              | 首页                   |
| /monitor       | jvm状态监控            |
| /api/condition | 运行一遍后查看商品状态 |

同时可以查看相关参数

![image-20220522162352240](http://coldairance.gitee.io/source/image-20220522162352240.png)

> 本地搭建（Linux系统）

克隆本项目的 dev 版本到本地，提前安装好各种环境：

* [前端](https://gitee.com/coldairance/STOnline/tree/dev/)
* [后端](https://github.com/coldairance/STServer/tree/dev)
* Docker（见下文）
* MySQL（见下文）
* Redis（见下文）
* RabbitMQ（见下文）
* **不需要安装Nginx**
* 修改`application.yml`文件，将`myself.jvmstart`路径修改正确

启动命令

```sh
// 前端
npm run dev
// 后端
略
```

浏览器输入`http://localhost:7000`，打开  /attach/ 目录的命令行，输入

```sh
jmeter -n -t Base.jmx
```

### 进阶

如果你想启动查看完整的数据分析，请为 Jmeter 安装 manager 插件（/test/jmeter-plugins-manager-1.7.jar），然后再打开`/test/Better.jmx`

### 自定义

>  修改/test/*.jmx 文件可以自定义请求模拟

* 修改 ip 地址 。
* 修改**开始模拟线程组**的线程数。
* 同时修改方案的api地址（见下文，三个都要修改为一样的）。

> 修改 /attach/data.py 文件可自定义数据。

## 架构设计

![image-20220522110102907](http://coldairance.gitee.io/source/image-20220522110102907.png)

> 硬件需求

一台云服务器

> 软件需求

Nginx Web服务器、Tomcat Web服务器、MySQL数据库、Redis数据库、任意消息队列、任意前端开发框架、任意后端开发框架。

> 需要解决的重要问题

* 并发同步问题。
* 限流/削峰问题。
* 消息重复消费、消息丢失。
* 持久化问题。
* ...

## 功能描述

### 功能

1. 下订单

2. 付款

### 前提约束

指定约束是为了更好的关注高并发设计中的问题：

* 由于没有授权认证模块，我们认为每一位用户都是合法的（即使是恶意攻击者）。
* 用户下单后不可以修改订单（锁单状态），只能选择付款或等待该订单失效。
* 一人仅可购买同一商品一件到三件（限购三件）。
* 同一商品买到了就不能再购买（限购一次），没买到的人可再次尝试购买（并发过大，服务器拒绝响应），且至多尝试一次（尝试多次没意义）就放弃购买。
* 付款时随机获得一个85折的优惠券（刺激消费）。
* 每一个用户所在地网络波动都不同（延时线程）。
* 每一位用户都准备了充足的钱用于购买商品。

## 项目搭建

### 表设计

#### 逻辑模型

![image-20220522112431171](http://coldairance.gitee.io/source/image-20220522112431171.png)

#### 物理模型

> 只关注秒杀相关

![image-20220522112522378](http://coldairance.gitee.io/source/image-20220522112522378.png)

### 缓存字段

| Key            | Value    | Value数据类型 | 说明                         |
| -------------- | -------- | ------------- | ---------------------------- |
| good-info      | 商品信息 | String        | 提前预加载所有商品信息       |
| good-count     | 商品库存 | String        | 单独加载商品库存             |
| good-order     | 商品订单 | String        | 缓存用户订单，减轻数据库压力 |
| rank           | 排行榜   | Zset          | 即时显示商品销售情况         |
| good-discounts | 折扣数量 | String        | 优惠券                       |

### 消息队列

| 队列名              | 模式 | 持久化 | 功能                 |
| ------------------- | ---- | ------ | -------------------- |
| db_order_queue      | P2P  | 是     | 存储数据库更新信息   |
| expired_order_queue | P2P  | 是     | 存储缓存订单过期信息 |
| dead_order_queue    | P2P  | 否     | 存储用户请求         |

### 消息代理

![20200828134900686.jpg](http://coldairance.gitee.io/source/20200828134900686.jpg)

使用 STOMP 协议代理 WebSocket 实现消息的全局推送。

### 自定义响应

| 响应码 | 消息         |
| ------ | ------------ |
| 200    | 成功         |
| 500    | 失败         |
| 501    | 订单重复     |
| 502    | 订单过期     |
| 503    | 重复购买     |
| 504    | 商品库存为空 |
| 505    | 服务繁忙     |

### 后端

#### 方案一

##### 原理

![image-20220510163436699](http://coldairance.gitee.io/source/image-20220510163436699.png)

优点：

* 订单暂存在缓存中减轻数据库压力。
* 先扣缓存库存再将订单写入数据库，可以保证不**超卖**。
* 数据库操作通过消息队列异步实现。

##### 接口开发

| 接口      | 参数                                                  | 功能           |
| --------- | ----------------------------------------------------- | -------------- |
| /reset    | store(Integer)：库存                                  | 重置数据库状态 |
| /v1/order | data(json)：{uid:用户id, gid:商品id, number:商品数量} | 添加订单       |
| /v1/pay   | data(json)：{uid:用户id, gid:商品id}                  | 支付订单       |

##### 问题

> 并发

* 对共享资源**库存**的竞争。
* 对共享资源**优惠券**的竞争。

**解决方式**：使用`synchronized`同步锁。

> 金额计算

* Double、Float会对精度有影响。
* 数据库的`DECIMAL`数据类型会对精度有影响。

**解决方式**：使用`String`类型保存浮点数，计算时转换为`BigDecimal`。

##### 流程图

![image-20220511104738132](http://coldairance.gitee.io/source/image-20220511104738132.png)

##### 测试

> 并发性测试

使用一个python脚本生成JMeter测试文件

```python
import random

orders = []
N = 20000
re = 0
# 生成 order 订单
for i in range(1, N+1):
    uid = i
    gid = random.randint(1, 6)
    number = random.randint(1, 3)
    s = f'{uid},{gid},{number}'
    orders.append(s)

    # 有人可能会购买其他小说
    if random.random()>0.5:
        uid = random.randint(1, i)
        gid = random.randint(1, 6)
        number = random.randint(1, 3)
        s = f'{uid},{gid},{number}'
        orders.append(s)
        re += 1
# 写入文件
file = open(f'./{N}-{re}.text', 'w')
for o in orders:
    file.write(o+'\n')
```

设置库存为 1000~2000，使用 JMeter 读取参数，进行测试

![image-20220509232227079](http://coldairance.gitee.io/source/image-20220509232227079.png)

如图，使用 100/s 的并发量进行测试。

**首次下订单**

![image-20220511165507818](http://coldairance.gitee.io/source/image-20220511165507818.png)

**第一次被拒绝后再次下订单**

![image-20220511165523779](http://coldairance.gitee.io/source/image-20220511165523779.png)

**支付**

![image-20220511165548247](http://coldairance.gitee.io/source/image-20220511165548247.png)

**分析**

首次请求异常包含重复购买、服务拒绝、库存不足。

再次抢购异常包含第二次被拒绝、库存不足。

付款异常包含库存不足、订单失效。

首次下订单拒绝了大部分用户，总体上只有不到 15% 的用户能进入支付界面，大多数对锁的争抢、数据库的访问都是有效的。

> 数据一致性校验

在 SpringBoot Test 里面

```java
@Test
public void consistent(){
    BigDecimal[] prices = new BigDecimal[7];
    prices[1] = new BigDecimal("19.99");
    prices[2] = new BigDecimal("24.99");
    prices[3] = new BigDecimal("9.99");
    prices[4] = new BigDecimal("14.99");
    prices[5] = new BigDecimal("39.99");
    prices[6] = new BigDecimal("69.99");

    int[] setout = new int[7];

    List<Good> goods = goodMapper.selectList(null);
    List<Order> orders = orderMapper.selectList(null);
    List<Receipt> receipts = receiptMapper.selectList(null);

    BigDecimal sum1 = new BigDecimal("0");
    BigDecimal sum2 = new BigDecimal("0");

    boolean error = false;

    for (Order order:
         orders) {
        setout[order.getGid()] += order.getNumber();
        sum1 = sum1.add(new BigDecimal(order.getDiscount()).multiply(prices[order.getGid()]).multiply(new BigDecimal(order.getNumber())));
    }

    for (Receipt receipt:
         receipts) {
        sum2 = sum2.add(new BigDecimal(receipt.getMoney()));
    }
    for (Good g:
         goods) {
        System.out.println("预定库存："+g.getNumber()+" 实际售出："+setout[g.getGid()]+" 缓存剩余："+redisUtil.get("GC-"+g.getGid()));
        error = g.getNumber().equals(setout[g.getGid()]+g.getGid());
    }
    System.out.println("订单计算总金额："+sum1);
    System.out.println("收据计算总金额："+sum2);
    error = error && (sum1.equals(sum2));
    System.out.println("是否出现异常："+((error)?"是":"否"));
}
```

```
// 结果
预定库存：1147 实际售出：1147 缓存剩余：0
预定库存：1147 实际售出：1147 缓存剩余：0
预定库存：1147 实际售出：1147 缓存剩余：0
预定库存：1147 实际售出：1147 缓存剩余：0
预定库存：1147 实际售出：1147 缓存剩余：0
预定库存：1147 实际售出：1147 缓存剩余：0
订单计算总金额：200211.0195
收据计算总金额：200211.0195
是否出现异常：否
```

#### 方案二

##### 原理

* 相较版本一，它的优点是提前库存的扣减，在**首次下订单**阶段能拒绝更多人的请求，进一步减轻后面阶段服务的压力。
* 对订单设置过期时间，过期了恢复缓存，由于 Redis 监听机制存在缺陷，因此选用 **rabbitmq+过期队列+死信队列** 进行实现（它的另一种更好实现是jdk自带的**DelayQueue**，不过需要自己做**持久化/恢复封装**）。这种机制存在**消息丢失**和**重复消费**问题，消息重复导致的**超卖**问题需要着重解决。
* 由于预扣订单没有加同步锁，预扣库存可能超出上限，支付时会导致较为严重的少卖问题。

![image-20220516193445144](http://coldairance.gitee.io/source/image-20220516193445144.png)

##### 接口开发

| 接口      | 参数                                                  | 功能           |
| --------- | ----------------------------------------------------- | -------------- |
| /reset    | store(Integer)：库存                                  | 重置数据库状态 |
| /v2/order | data(json)：{uid:用户id, gid:商品id, number:商品数量} | 添加订单       |
| /v2/pay   | data(json)：{uid:用户id, gid:商品id}                  | 支付订单       |

##### 流程图

![image-20220511104520775](http://coldairance.gitee.io/source/image-20220511104520775.png)

##### 测试

当设置过期时间为 3s 时。

```sh
预定库存：1147 实际售出：1113 缓存剩余：34
预定库存：1147 实际售出：1118 缓存剩余：29
预定库存：1147 实际售出：1100 缓存剩余：47
预定库存：1147 实际售出：1093 缓存剩余：54
预定库存：1147 实际售出：1104 缓存剩余：43
预定库存：1147 实际售出：1076 缓存剩余：71
订单计算总金额：190745.7740
收据计算总金额：190745.7740
是否出现异常：否
```

可以看见出现了不同程度的少卖现象

#### 方案三

* 是方案二的另一种实现，在下订单阶段直接加锁保证不会出现**超卖**现象，同时解决了方案二的**少卖**现象。

![image-20220516192151398](http://coldairance.gitee.io/source/image-20220516192151398.png)

##### 接口开发

| 接口      | 参数                                                  | 功能           |
| --------- | ----------------------------------------------------- | -------------- |
| /reset    | store(Integer)：库存                                  | 重置数据库状态 |
| /v3/order | data(json)：{uid:用户id, gid:商品id, number:商品数量} | 添加订单       |
| /v3/pay   | data(json)：{uid:用户id, gid:商品id}                  | 支付订单       |

### 前端

#### 功能

#### 前台

* 商品展示。
* 销量排行榜，动态更新。
* 推送消息框，动态更新。

#### 后台

* 应用程序、服务器状态的简单动态监控。

* 动态修改应用参数。

#### 系统

* 使用Stomp协议监听后端传来的异步消息。

* 对用户请求进行限流。

#### 接口开发

| 接口     | 参数 | 功能         |
| -------- | ---- | ------------ |
| /        |      | 前台         |
| /monitor |      | 后台控制面板 |

### 服务限流

> 用户限流-令牌桶算法

* 在 Tomcat 入口处限制进入的请求流量。

* 在下订单阶段限制同一个用户（黄牛）短时间内不正常的频繁访问行为，key 为用户的唯一id（ip）。

```sh
Lua脚本大致逻辑如下：

-- 获取调用脚本时传入的第一个key值（用作限流的 key）
local key = KEYS[1]
-- 获取调用脚本时传入的第一个参数值（限流大小）
local limit = tonumber(ARGV[1])

-- 获取当前流量大小
local curentLimit = tonumber(redis.call('get', key) or "0")

-- 是否超出限流
if curentLimit + 1 > limit then
    -- 返回(拒绝)
    return 0
else
    -- 没有超出 value + 1
    redis.call("INCRBY", key, 1)
    -- 设置过期时间
    redis.call("EXPIRE", key, 2)
    -- 返回(放行)
    return 1
end
```

> 接口限流-漏桶算法

控制后端接口连接数，通过 Nginx 服务器配置（见下文）。

### 状态监控

本系统支持一些内存状态的动态监听...

| 属性         | 描述         |
| ------------ | ------------ |
| jvm.memory   | JVM 内存状态 |
| jvm.gc.pause | JVM GC 状态  |

## 项目上线

### 硬件规格

* CPU：2核
* 内存：4GB
* 系统盘：SSD 60GB
* 带宽：6Mbps

### 端口开放与限制

![image-20220520154136548](http://coldairance.gitee.io/source/image-20220520154136548.png)

### 工具安装

#### Dokcker

https://docs.docker.com/desktop/linux/install/

#### Nginx

```sh
apt-get install nginx
nginx -v
service nginx start
service nginx restart
service nginx stop
```

欢迎页:host:80/

配置代理信息

```shell
touch /etc/nginx/conf.d/st.conf
vim /etc/nginx/conf.d/st.conf
```

```json
limit_conn_zone $server_name zone=perserver:10m;
server {
    # 监听80端口
    listen 80;
    server_name 101.42.164.61;
    location / {
            root   /home/STOnline/dist;
            try_files $uri $uri/ /index.html;
            index  index.html;
    }
	# 转发后端请求
    location /api/ {
        # 限制流量为1000
        limit_conn perserver 1000;
        proxy_pass http://127.0.0.1:8081/;
    }
}
```

#### Maven

```shell
apt-get install maven
mvn -v
```

修改镜像

```sh
vim /etc/maven/settings.xml

<mirror>
    <id>alimaven</id>
    <name>aliyun maven</name>
    <url>http://maven.aliyun.com/nexus/content/groups/public/</url>
    <mirrorOf>central</mirrorOf>
</mirror>
```

#### NodeJS

```sh
wget https://cdn.npmmirror.com/binaries/node/v16.15.0/node-v16.15.0-linux-x64.tar.xz
tar xvJf node-v16.15.0-linux-x64.tar.xz
```

#### JDK

```sh
wget -c https://repo.huaweicloud.com/java/jdk/8u181-b13/jdk-8u181-linux-x64.tar.gz
tar -zxvf jdk-8u181-linux-x64.tar.gz
```

#### 配置环境变量

```sh
vim /etc/profile

###########
export JAVA_HOME=/home/env/jdk8
export NODE_HOME=/home/env/node16
export PATH=$JAVA_HOME/bin:$NODE_HOME/bin:$PATH
###########

source /etc/profile
java -version
node -v
```

### Docker容器安装

#### MySQL

```shell
# 创建挂载文件并赋予权限
mkdir -p /home/docker-data/mysql/{data,conf,log} \
chmod -R 777 /home/docker-data/mysql
# 启动
docker run --name mysql -p 3306:3306 \
-v /home/docker-data/mysql/conf:/etc/mysql \
-v /home/docker-data/mysql/log:/var/log/mysql \
-v /home/docker-data/mysql/data:/var/lib/mysql \
-e MYSQL_ROOT_PASSWORD=123456 \
-d mysql:5.7
```

#### Redis

```shell
# 创建挂载文件并赋予权限
mkdir -p /home/docker-data/redis/{data,conf,log} \
chmod -R 777 /home/docker-data/redis

# 设置配置文件
touch /home/docker-data/redis/conf/redis.conf
vim /home/docker-data/redis/conf/redis.conf

#####redis.conf
bind 127.0.0.1 #注释掉这部分，使redis可以外部访问
daemonize no #用守护线程的方式启动，默认是no
requirepass 123456#给redis设置密码
appendonly no#redis持久化　　默认是no
tcp-keepalive 300 #防止出现远程主机强迫关闭了一个现有的连接的错误 默认是0
###############

# 启动
docker run -p 6379:6379 --name redis \
-v /home/docker-data/redis/conf:/etc/redis \
-v /home/docker-data/redis/log:/var/log/redis \
-v /home/docker-data/redis/data:/var/lib/redis \
-d redis:7.0.0 redis-server
```

#### RabbitMQ

```shell
# 创建挂载文件并赋予权限
mkdir -p /home/docker-data/rabbitmq/{data,conf,log} \
chmod -R 777 /home/docker-data/rabbitmq
# 启动 5672：service 15672：web gui 61613：proxy 15674：proxy web
docker run --name rabbitmq \
-p 5672:5672 -p 15674:15674 -p 15672:15672 -p 61613:61613 \
-v /home/docker-data/rabbitmq/conf:/etc/rabbitmq \
-v /home/docker-data/rabbitmq/log:/var/log/rabbitmq \
-v /home/docker-data/rabbitmq/data:/var/lib/rabbitmq \
-d rabbitmq:3.10
# 开启插件
docker exec -it rabbitmq rabbitmq-plugins enable rabbitmq_web_stomp
# 添加用户
docker exec -it rabbitmq rabbitmqctl add_user admin admin
# 赋予权限
docker exec -it rabbitmq rabbitmqctl set_permissions -p / admin ".*" ".*" ".*"
# 赋予管理员角色
docker exec -it rabbitmq rabbitmqctl set_user_tags admin administrator
```

访问：host:15672/

## 测试

### Jmeter插件安装

Jmeter 原始提供的图形很差，并且没有TPS图标，所以需要配合插件使用，才能看到清楚的响应时间图形、TPS、以及线程数；

1. [插件管理工具](https://jmeter-plugins.org/wiki/PluginsManager/)
2. 3 Basic Graphs
3. 5 Additional Graphs

监控指标有：

* Transactions per Second：每秒事务处理量。
* Response Codes per Second：每秒返回的响应码。
* Response Times Over Time：每个样本的平均响应时间。
* Conenect Times Over Time：在负载测试期间发送请求后与服务器建立连接的平均时间。

### 限流测试

#### 单用户/入口限流

通过一个数据为`单用户多次购买`的数据文件进行这项测试，访问次数为**100**，线程数为**10**，文件名为`single_user_limit.txt`

当limit为**3**时，它的部分结果为

![image-20220521154707348](http://coldairance.gitee.io/source/image-20220521154707348.png)

当limit为**10**，它的部分结果为

![image-20220521155636448](http://coldairance.gitee.io/source/image-20220521155636448.png)

#### nginx限流

通过一个数据为`多用户多次购买`的数据文件进行这项测试，访问次数为**1000**，limit为**10**，同时只开启下订单接口测试，文件名为`port_limit.txt`。

为了保证大部分

连接都是1s以上的长连接，我在同步代码块中让线程休眠1000s，对应的代码为

```java
...
synchronized (this){
    Thread.sleep(1000);
    // 检查库存
    Integer cnt = (Integer) redisUtil.get("GC-" + order.getGid());
    if(cnt<order.getNumber()){
        return new Result(ResultCode.GOOD_EMPTY);
    }
    redisUtil.decr("GC-" + order.getGid(),order.getNumber());
}
...
```

当线程数为100时，结果为

![image-20220521212319668](http://coldairance.gitee.io/source/image-20220521212319668.png)

当线程数为400时，结果为

![image-20220521211939342](http://coldairance.gitee.io/source/image-20220521211939342.png)

可以看到，限流生效了

### 数据测试

统一使用`data.txt`进行测试，线程数为100

#### 方案一

![image-20220521222537107](http://coldairance.gitee.io/source/image-20220521222537107.png)

商品在47秒左右就已经全部卖完

```json
[
    {
        实际售出：: "库存：1147 优惠券：114",
        预订：: "库存：1147 优惠券：114",
        商品名：: "浅濑神社",
        缓存剩余：: "库存：0 优惠券：0"
    },
    {
        实际售出：: "库存：1147 优惠券：114",
        预订：: "库存：1147 优惠券：114",
        商品名：: "龙脊雪山",
        缓存剩余：: "库存：0 优惠券：0"
    },
    {
        实际售出：: "库存：1147 优惠券：114",
        预订：: "库存：1147 优惠券：114",
        商品名：: "绝云间",
        缓存剩余：: "库存：0 优惠券：0"
    },
    {
        实际售出：: "库存：1147 优惠券：114",
        预订：: "库存：1147 优惠券：114",
        商品名：: "风龙废墟",
        缓存剩余：: "库存：0 优惠券：0"
    },
    {
        实际售出：: "库存：1147 优惠券：114",
        预订：: "库存：1147 优惠券：114",
        商品名：: "层岩巨渊",
        缓存剩余：: "库存：0 优惠券：0"
    },
    {
        实际售出：: "库存：1147 优惠券：114",
        预订：: "库存：1147 优惠券：114",
        商品名：: "鸣神大社",
        缓存剩余：: "库存：0 优惠券：0"
    },
    {
        是否出现异常：: "否",
        订单计算总金额：: 211380.2425,
        收据计算总金额：: 211380.2425
    }
]
```

![image-20220521224519766](http://coldairance.gitee.io/source/image-20220521224519766.png)

#### 方案二

![image-20220521223243799](http://coldairance.gitee.io/source/image-20220521223243799.png)

商品从头到尾一直都有货，但会出现少卖的情况

```json
[
    {
        实际售出：: "库存：1127 优惠券：114",
        预订：: "库存：1147 优惠券：114",
        商品名：: "浅濑神社",
        缓存剩余：: "库存：19 优惠券：0"
    },
    {
        实际售出：: "库存：1129 优惠券：114",
        预订：: "库存：1147 优惠券：114",
        商品名：: "龙脊雪山",
        缓存剩余：: "库存：16 优惠券：0"
    },
    {
        实际售出：: "库存：1132 优惠券：114",
        预订：: "库存：1147 优惠券：114",
        商品名：: "绝云间",
        缓存剩余：: "库存：14 优惠券：0"
    },
    {
        实际售出：: "库存：1136 优惠券：114",
        预订：: "库存：1147 优惠券：114",
        商品名：: "风龙废墟",
        缓存剩余：: "库存：9 优惠券：0"
    },
    {
        实际售出：: "库存：1131 优惠券：114",
        预订：: "库存：1147 优惠券：114",
        商品名：: "层岩巨渊",
        缓存剩余：: "库存：13 优惠券：0"
    },
    {
        实际售出：: "库存：1133 优惠券：114",
        预订：: "库存：1147 优惠券：114",
        商品名：: "鸣神大社",
        缓存剩余：: "库存：13 优惠券：0"
    },
    {
        是否出现异常：: "是",
        订单计算总金额：: 208299.955,
        收据计算总金额：: 208299.955
    }
]
```

![image-20220521224022314](http://coldairance.gitee.io/source/image-20220521224022314.png)

#### 方案三

![image-20220521225049026](http://coldairance.gitee.io/source/image-20220521225049026.png)

商品数量逐渐变少，直至到1:32左右为空

```json
[
    {
        实际售出：: "库存：1147 优惠券：114",
        预订：: "库存：1147 优惠券：114",
        商品名：: "浅濑神社",
        缓存剩余：: "库存：0 优惠券：0"
    },
    {
        实际售出：: "库存：1147 优惠券：114",
        预订：: "库存：1147 优惠券：114",
        商品名：: "龙脊雪山",
        缓存剩余：: "库存：0 优惠券：0"
    },
    {
        实际售出：: "库存：1147 优惠券：114",
        预订：: "库存：1147 优惠券：114",
        商品名：: "绝云间",
        缓存剩余：: "库存：0 优惠券：0"
    },
    {
        实际售出：: "库存：1147 优惠券：114",
        预订：: "库存：1147 优惠券：114",
        商品名：: "风龙废墟",
        缓存剩余：: "库存：0 优惠券：0"
    },
    {
        实际售出：: "库存：1147 优惠券：114",
        预订：: "库存：1147 优惠券：114",
        商品名：: "层岩巨渊",
        缓存剩余：: "库存：0 优惠券：0"
    },
    {
        实际售出：: "库存：1147 优惠券：114",
        预订：: "库存：1147 优惠券：114",
        商品名：: "鸣神大社",
        缓存剩余：: "库存：0 优惠券：0"
    },
    {
        是否出现异常：: "否",
        订单计算总金额：: 211261.783,
        收据计算总金额：: 211261.783
    }
]
```

![image-20220521225318342](http://coldairance.gitee.io/source/image-20220521225318342.png)

RTT较前二者明显升高。

### 负载测试

线程数提高到1000，测试方案三

![image-20220521230019157](http://coldairance.gitee.io/source/image-20220521230019157.png)

![image-20220521225948900](http://coldairance.gitee.io/source/image-20220521225948900.png)

并发量过高导致Nginx拒绝服务次数增多，进而导致支付失败增多，服务器压力不断增长。导致部分连接关闭缓慢。

![image-20220521230513101](http://coldairance.gitee.io/source/image-20220521230513101.png)

### 压力测试

线程数增加到2000

![image-20220521231529418](http://coldairance.gitee.io/source/image-20220521231529418.png)

Nginx 限流发挥作用

![image-20220521231455769](http://coldairance.gitee.io/source/image-20220521231455769.png)

服务器稳定性进一步降低，部分连接关闭异常。

```json
[
    {
        实际售出：: "库存：960 优惠券：114",
        预订：: "库存：1147 优惠券：114",
        商品名：: "浅濑神社",
        缓存剩余：: "库存：187 优惠券：0"
    },
    {
        实际售出：: "库存：947 优惠券：114",
        预订：: "库存：1147 优惠券：114",
        商品名：: "龙脊雪山",
        缓存剩余：: "库存：200 优惠券：0"
    },
    {
        实际售出：: "库存：950 优惠券：114",
        预订：: "库存：1147 优惠券：114",
        商品名：: "绝云间",
        缓存剩余：: "库存：197 优惠券：0"
    },
    {
        实际售出：: "库存：957 优惠券：114",
        预订：: "库存：1147 优惠券：114",
        商品名：: "风龙废墟",
        缓存剩余：: "库存：190 优惠券：0"
    },
    {
        实际售出：: "库存：953 优惠券：114",
        预订：: "库存：1147 优惠券：114",
        商品名：: "层岩巨渊",
        缓存剩余：: "库存：194 优惠券：0"
    },
    {
        实际售出：: "库存：935 优惠券：114",
        预订：: "库存：1147 优惠券：114",
        商品名：: "鸣神大社",
        缓存剩余：: "库存：212 优惠券：0"
    },
    {
        是否出现异常：: "否",
        订单计算总金额：: 174162.2205,
        收据计算总金额：: 174162.2205
    }
]
```

出现严重的少卖现象

## JVM调优

使用`stable.txt`进行测试，线程数设置为**400**，库存设置为**11470**，为保证系统吞吐量，GC指定为`ParallelGC`

目前参数

```
Xmx2048m
-Xms2048m
-Xss256k
-XX:SurvivorRatio=8
-XX:TargetSurvivorRatio=50
-XX:NewRatio=1
-XX:InitialTenuringThreshold=5
-xx:MaxTenuringThreshold=5
-XX:+UseParallelGC
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/home/jvm.dump
```

测试前 GC state

| 类别  | 次数 | 平均时间（秒） |
| ----- | ---- | -------------- |
| minor | 4    | 0.0280         |
| major | 1    | 0.1910         |

测试后 GC state

| 类别  | 次数 | 平均时间（秒） |
| ----- | ---- | -------------- |
| minor | 133  | 0.0111         |
| major | 2    | 0.2765         |

老年代GC时间明显高于年轻代GC，总GC时长为$1.4763(minor)+0.554(major)=2.0293$

尝试调整 TargetSurvivorRatio、NewRatio、TenuringThreshold

```
Xmx2048m
-Xms2048m
-Xss256k
-XX:SurvivorRatio=8
-XX:TargetSurvivorRatio=80
-XX:NewRatio=2
-XX:InitialTenuringThreshold=10
-xx:MaxTenuringThreshold=10
-XX:+UseParallelGC
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/home/jvm.dump
```

测试前 GC state

| 类别  | 次数 | 平均时间（秒） |
| ----- | ---- | -------------- |
| minor | 2    | 0.0465         |
| major | 1    | 0.2500         |

测试后 GC state

| 类别  | 次数 | 平均时间（秒） |
| ----- | ---- | -------------- |
| minor | 68   | 0.0151         |
| major | 1    | 0.2500         |

总GC时长为$1.4763(minor)+0.2500(major)=1.2768$

## 后话

项目还有许多不完善的地方，仅本人熟悉技术所准备的一个面试项目，未解决的问题有：

* 消息队列持久化以及恢复
* 消息重复消费、消息积压、消息丢失
* 数据库备份以及恢复
* 表结构优化、索引添加等
* 分布式优化方案
* ...

由于开发过程匆忙（2周），系统多少会有一些bug，如有疑问，请在 **github** 提出问题，我会尽快回复。

## 参考

https://blog.csdn.net/zhizhengguan/article/details/121236452

https://blog.csdn.net/code_javaer/article/details/120476249

https://blog.csdn.net/weixin_49029722/article/details/120542499

https://wenku.baidu.com/view/ad57f23a5aeef8c75fbfc77da26925c52cc5910a.html

https://blog.csdn.net/Michaelyq1229/article/details/120817361

https://www.cnblogs.com/xbxblog/p/9602885.html

https://www.cnblogs.com/lonely-wolf/p/14368920.html

https://www.cnblogs.com/caoweixiong/p/15325382.html

https://blog.csdn.net/u013347843/article/details/122100941

http://www.javashuo.com/article/p-khbaxgmf-mm.html

https://blog.csdn.net/weixin_58495461/article/details/119718583

https://blog.csdn.net/qq_35387940/article/details/108276136

https://blog.csdn.net/fu_zhongyuan/article/details/87973174

https://blog.csdn.net/foolcuntry/article/details/118224194

https://www.jianshu.com/p/184db4b3e134

https://blog.csdn.net/qq_41893274/article/details/116573250

https://blog.csdn.net/qq_46416934/article/details/123953382

https://blog.csdn.net/qq_36080515/article/details/120502104

https://blog.csdn.net/houkai18792669930/article/details/114173349

https://baidaguo.blog.csdn.net/article/details/121397268?spm=1001.2101.3001.6650.7&utm_medium=distribute.pc_relevant.none-task-blog-2%7Edefault%7EOPENSEARCH%7Edefault-7-121397268-blog-114173349.pc_relevant_eslanding_v3&depth_1-utm_source=distribute.pc_relevant.none-task-blog-2%7Edefault%7EOPENSEARCH%7Edefault-7-121397268-blog-114173349.pc_relevant_eslanding_v3&utm_relevant_index=12

https://blog.csdn.net/weixin_39430584/article/details/80947093?spm=1001.2101.3001.6650.1&utm_medium=distribute.pc_relevant.none-task-blog-2%7Edefault%7ECTRLIST%7Edefault-1-80947093-blog-124144300.pc_relevant_scanpaymentv1&depth_1-utm_source=distribute.pc_relevant.none-task-blog-2%7Edefault%7ECTRLIST%7Edefault-1-80947093-blog-124144300.pc_relevant_scanpaymentv1&utm_relevant_index=1

https://www.szhjjp.com/n/21125.html

[Java8 JVM 参数文档](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/java.html)

