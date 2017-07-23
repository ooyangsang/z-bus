# ZBUS = MQ + RPC

* [Project Target](http://git.oschina.net/rushmore/zbus#zbus解决的问题域 "") 
* [Features](http://git.oschina.net/rushmore/zbus#zbus特点 "")  
* [Startup](http://git.oschina.net/rushmore/zbus#zbus启动 "") 
* [Message Queue (MQ)](http://git.oschina.net/rushmore/zbus#zbus实现消息队列 "") 
* [Remote Procedure Call (RPC)](http://git.oschina.net/rushmore/zbus#zbus实现RPC "") 
* [Service Bus](http://git.oschina.net/rushmore/zbus#zbus实现异构服务代理--服务总线 "") 
* [Client API](http://git.oschina.net/rushmore/zbus#zbus底层编程扩展 "") 
* [High Availability (HA)](http://git.oschina.net/rushmore/zbus#zzbus高可用模式 "") 
* [Performance](http://git.oschina.net/rushmore/zbus#zbus性能测试数据 "") 


##Project Target
1. Messaging Queue(MQ) -- Decouple of application dependency
2. Remote Procedure Call(RPC) 
3. Service Bus -- RPC/protocol adaptor to different platforms

##NOT Support
1. Distributed Transaction

##Features
1. Extremely light-weighted, distributed around 300k size
2. Message Persistence, billions of message write instance
3. High Availability(HA) support.
4. Multiple languages/platforms--JAVA/C/C++/C#/Python/Node.JS 
5. Raw HTTP header-extended protocol support to write new SDK

##Discussion QQ Group：467741880  


##Startup

zbus is a broker for message queuing, it runs in distributed mode by default, and of course it  could also run in embedded mode along with the main process.

** 1.Distributed mode **

  zbus startup is very simple, choose the shell files in zbus-dist directory of the project, zbus.bat for windows and zbus.sh for linux/mac. If configuration is required, edit the shell file to adjust JVM parameters or MQ related configuration such as store path for message persistence. If startup does go wrong, check out (1) whether JVM is properly configured, (2) whether the server port is in use, and in most cases the problem should be solved.
	
** 2.Embedded mode **

	MqServerConfig config = new MqServerConfig();   
	config.serverPort = 15555;  
	config.storePath = "./store";  
	final MqServer server = new MqServer(config);  
	server.start();  

zbus monitor could be accessed from the web browser via http://localhost:15555 by default

![zbus监控](http://git.oschina.net/uploads/images/2016/0316/161713_c15c6031_7458.png "zbus监控")	

##Message Queue(MQ)

Messaging Queue(MQ) is the core function of zbus, MQ application consists of three roles:

1. Broker -- intermediate server dealing message for producer and consumer
2. Producer -- the client producing message
3. Consumer -- the client consuming message

	Producer ==> Broker ==> Consumer

MQ usually plays the role of decoupling application dependency

逻辑上解耦分离
1. 生产者只需要知道Broker的存在，负责生产消息到Broker，不需要关心消费者的行为
2. 消费者也只需要知道Broker的存在，负责消费处理Broker上某个MQ队列的消息，不需要关心生产者的行为

不同的Broker实现在细节上会有些不同，但是在MQ逻辑解耦上基本保持一致，下面细节全部是以zbus特定定义展开

zbus与客户端（生产者与消费者）之间通讯的消息（org.zbus.net.http.Message）为了扩展性采用了【扩展HTTP】消息格式。
zbus的消息逻辑组织是以MQ标识来分组消息，MQ标识在zbus中就是MQ名字，Message对象中可以直接指定。
物理上zbus把同一个下MQ标识下的消息按照FIFO队列的模式在磁盘中存储，队列长度受限于磁盘大小，与内存无关。

编程模型上，分两个视图，生产者与消费者两个视图展开

1. 生产者视图
2. 消费者视图

生产者与消费者在编程模型上都需要首先产生一个Broker，Broker是对zbus本身的抽象，为了达到编程模型的一致，Broker可以是
单机版本的SingleBroker，也可以是高可用版本的HaBroker，甚至可以是不经过网络的本地化JvmBroker，这些类型的Broker都是不同的实现，编程模型上不关心，具体根据实际运行环境而定，为了更加方便配置，ZbusBroker实现了上述几种不同的Broker实现的代理包装，根据具体Broker地址来决定最终的版本。

例如

	Broker broker = new ZbusBroker("127.0.0.1:15555"); //SingleBroker
	Broker broker = new ZbusBroker("127.0.0.1:16666;127.0.0.1:16667"); //HaBroker
	Broker broker = new ZbusBroker("jvm"); //JvmBroker

Broker内部核心实现了：
1. 连接池管理
2. 同步异步API

所以Broker在JAVA中可以理解为类似JDBC连接池一样的重对象，应该共享使用，大部分场景应该是Application生命周期。
而依赖Broker对象而存在的Producer与Consumer一般可以看成是轻量级对象（Consumer因为拥有链接需要关闭）


**生产消息**
		
	//Producer是轻量级对象可以随意创建不用释放 
	Producer producer = new Producer(broker, "MyMQ");
	producer.createMQ();//确定为创建消息队列需要显示调用

	Message msg = new Message();
	msg.setBody("hello world");  //消息体底层是byte[]
	msg = producer.sendSync(msg);

**消费消息**

	Consumer consumer = new Consumer(broker, "MyMQ");  
	consumer.start(new ConsumerHandler() { 
		@Override
		public void handle(Message msg, Consumer consumer) throws IOException { 
			//消息回调处理
			System.out.println(msg);
		}
	}); 
	//可控的范围内需要关闭consumer（内部拥有了物理连接）

生产者可以异步发送消息，直接调用producer.sendAsync()，具体请参考examples中相关示例

消费者可以使用更底层的API控制怎么取消息，直接调用consumer.take()从zbus上取回消息
    
从上面的API来看，使用非常简单，连接池管理，同步异步处理、高可用等相关主题全部留给了Broker抽象本身


###消息队列扩展主题

消息严格顺序问题
参考文章
* [高可用保证消息绝对顺序消费的BROKER设计方案](http://www.infoq.com/cn/articles/high-availability-broker-design "")



##zbus实现RPC

MQ消息队列用于解耦应用之间的依赖关系，一般认为MQ是从更广泛的分布式RPC中演变而来的：在RPC场景下，如果某个远程方法调用耗时过长，调用方不希望blocking等待，除了异步处理之外，更加常见的改造方式是采用消息队列解耦调用方与服务方。

RPC的场景更加常见，RPC需要解决异构环境跨语言的调用问题，有非常多的解决方案，综合看都是折中方案，zbus也属其一。


RPC从数据通讯角度来看可以简单理解为：


	分布式调用方A --->命令序列化(method+params) ---> 网络传输 --->  分布式式服务方B 命令反序列化（method+params）
	       ^                                                                            | 
    	   |                                                                            v
    	   |<---结果反序列化(result/error)<----- 网络传输 <----结果序列化(result/error) <---调用本地方法


网络传输可以是纯网络传递消息也可以是其他服务器中转，比如消息队列


异构环境下RPC方案需要解决的问题包括以下核心问题

	1. 跨语言，多语言平台下的消息通讯格式选择问题
	2. 服务端伺服问题，高性能处理模型
	3. 分布式负载均衡问题

WebService采用HTTP协议负载，SOAP跨语言描述对象解决问题1

Windows WCF采用抽象统一WebService和私有序列化高效传输解决问题1

在服务端处理模型与分布式负载均衡方面并不多体现，这里不讨论WebService，WCF或者某些私有的RPC方案的优劣之分，工程优化过程中出现了诸如Thrift，dubbo等等RPC框架，折射出来是的对已有的RPC方案中折中的不满。


针对问题1，zbus的RPC采用的是JSON数据根式封装跨语言平台协议，特点是简单明了，协议应用广泛（zbus设计上可以替换JSON）

针对问题2、问题3，zbus默认采用两套模式，MQ-RPC与DirectRPC， MQ-RPC基于MQ消息队列集中接入模式，DirectRPC则通过交叉直连模式

zbus的RPC方案除了解决上面三个问题之外，还有两个重要的工程目标：

	4. 极其轻量、方便二次开发
	5. RPC业务本身与zbus解耦（无侵入，方便直接替换掉zbus）


zbus的RPC设计非常简单，模型上对请求和应答做了基本的抽象

	public static class Request{ 
		private String module = ""; //模块标识
		private String method;      //远程方法
		private Object[] params;    //参数列表
		private String[] paramTypes;
		private String encoding = "UTF-8";
	}
	
	public static class Response {  
		private Object result;  
		private Throwable error;
		private String stackTrace; //异常时候一定保证stackTrace设定，判断的逻辑以此为依据
		private String encoding = "UTF-8";
	}

非常直观的抽象设计，就是对method+params 与 结果result/error 的JAVA表达而已。

RpcCodec的一个JSON协议实现---JsonRpcCodec完成将上述对象序列化成JSON格式放入到HTTP消息体中在网络上传输

###RPC调用方

RpcInvoker API核心
	
	public class RpcInvoker{ 
		private MessageInvoker messageInvoker; 
		private RpcCodec codec; //RPC对象序列化协议
		
		public Response invokeSync(Request request){
			.....
		} 
	}
完成将上述请求序列化并发送至网络，等待结果返回，序列化回result/error。

	//调用示例
	RpcInvoker rpc = new RpcInvoker(...); //构造出RpcInvoker
	
	//利用RpcInvoker 调用方法echo(String msg), 给定参数值 "test"
	
	//1) 调用动态底层API
	Request request = new Request();
	request.setMethod("echo");
	request.setParams(new Object[]{"test"});
	Response response = rpc.invokeSync(request);
	
	//2）强类型返回结果
	String echoString = rpc.invokeSync(String.class, "echo", "test"); 
	
	

RpcInvoker同时适配MQ-RPC与DirectRPC，只需要给RpcInvoker指定不同的底层消息MessageInvoker，比如

1. 点对点DirectRPC (MessageClient/Broker) 
2. 高可用DirectRPC (HaInvoker)
3. MQ-RPC         (MqInvoker)

点对点DirectRPC
	
	//1） MessageClient是一种MessageInvoker，物理连接点对点
	MessageInvoker client = new MessageClient("127.0.0.1:15555", ....);
	RpcInvoker rpc = new RpcInvoker(client); //构造出RpcInvoker 
	
	//2) Broker也是一种MessageInvoker, 因为Broker管理了连接池，这样构造的RpcInvoker具有连接池能力
	MessageInvoker broker = new ZbusBroker("127.0.0.1:15555"); 
	RpcInvoker rpc = new RpcInvoker(broker); //构造出RpcInvoker 
	
	//1)与2）本质上都是点对点的直连模式
	
高可用DirectRPC
	
	//1） 接入到Trackserver的ZbusBroker，具备高可用选择能力
	MessageInvoker messageInvoker = new ZbusBroker("127.0.0.1:16666;127.0.0.1:16667");
	//指定高可用服务器上的选择标识，注册为相同标识的服务提供方之间高可用
	HaInvoker haInvoker = new HaInvoker(messageInvoker, "HaDirectRpc"); 
	RpcInvoker rpc = new RpcInvoker(haInvoker); //构造出RpcInvoker 
	

MQ-RPC
		
	//step 1 生成一个到zbus服务器的MessageInvoker
	Broker broker = new ZbusBroker(); 
	//step 2 类似Java IoStream封装，在点对点基础上可以适配出MQ能力的MessageInvoker
	MessageInvoker mqInvoker = new MqInvoker(broker, "MyRpc"); //使用某个队列实现的RPC，调用适配
	RpcInvoker rpc = new RpcInvoker(mqInvoker); //构造出RpcInvoker 

以上三种RPC结构优缺点如下：
* 点对点DirectRPC简单单机性能高，但存在单点问题
* 高可用DirectRPC解决点对点的单点问题，但是网络连接是蜘蛛网状
* MQ-RPC集中式管理，多机负载均衡，但是因为所有消息都走了中间节点，性能有所下降



为了解决问题5，使得zbus在RPC业务解耦，zbus增加了动态代理类

RpcFactory API完成业务interface经过zbus的RPC动态代理类实现

	public class RpcFactory {
		private final MessageInvoker messageInvoker; //底层支持的消息Invoker，完成动态代理 	
		public <T> T getService(Class<T> api) throws Exception{
			....
		}
	}

通过RpcFactory则完成了业务代码与zbus的解耦（通过spring等IOC容器更加彻底的把zbus完全隔离掉）

	
	MessageInvoker invoker = new ... //DirectRPC或者MqRPC 选择， 同上
	//RpcFactory根据底层invoker来决定消息流
	RpcFactory factory = new RpcFactory(invoker);   
	//动态生成出InterfaceExample的实现类，RPC调用方不需要真正的实现类，客户端与服务端都通interface解耦
	InterfaceExample hello = factory.getService(InterfaceExample.class);

Spring的配置完全是上述代码的XML翻译，在此不做例子，具体参考examples下spring配置示例。



###RPC服务方

RPC数据流图中分布式服务提供方需要的两件事情是

1. 如何拿到请求RPC数据包
2. 解释好包如何调动本地对应的方法

对于问题1.如何拿到数据包，分两大类处理方案：DirectRPC与MQ-RPC

DirectRPC则需要启动网络侦听服务，被动处理请求RPC包；MQ-RPC则是使用Consumer从zbus的MQ队列中主动取RPC请求包。

DirectRPC的服务zbus采用JAVA NIO服务器完成，对应org.zbus.rpc.direct.Service服务器完成NIO网络伺服；MQ-RPC对应org.zbus.rpc.mq.Service，多Consumer线程从zbus的某个MQ队列中并发取RPC请求包。

对于问题2，不管哪种模式的RPC都采用相同的处理方式--RpcProcessor

	public class RpcProcessor implements MessageProcessor{ 
		private RpcCodec codec = new JsonRpcCodec(); //序列反序列化Request/Response
		private Map<String, MethodInstance> methods = new HashMap<String, MethodInstance>();  //业务方法映射表
		
		public void addModule(String module, Object... services){
			.....
		}
		public Message process(Message msg){ 
			.....
		}
	}
	
RpcProcessor本质上是通过反射将业务逻辑对象中的方法组织成 method==>(Method对象,Instance)映射

RpcProcessor.addModule(module, BizObject...)完成这个映射的管理

process的过程如下：

	1. 处理RPC的请求包,RpcCodec反序列化出Request对象
	2. 根据Request对象找到合适的Method并尝试调用
	3. 调用结果组装成合适的Response对象
	4. RpcCodec反序列化Response对象返回RPC响应包


启动RPC服务在zbus中变得非常简单，分两步完成

	//1)构造RpcProcessor--准备好服务映射表 
	RpcProcessor processor = new RpcProcessor();  
	processor.addModule(new InterfaceExampleImpl()); //动态增加业务对象，提供真正的业务逻辑
	
	
	//2)MQ-RPC或者DirectRPC的Service--容器运行上面的RpcProcessor
	ServiceConfig config = new ServiceConfig();
	config.setMessageProcessor(processor);  
	//更多的配置
	Service svc = new Service(config);
	svc.start();  

Spring的配置完全是上述代码的XML翻译，在此不做例子，具体参考examples下spring配置示例。


##zbus实现异构服务代理--服务总线

ZBUS = MQ+RPC

![zbus总线](http://git.oschina.net/uploads/images/2016/0316/160036_48bf3b72_7458.png "zbus总线") 

跨平台多语言+集中式节点控制，使得zbus适合完成服务总线适配工作。

为什么要采用总线架构适配已有服务？
1. 集中式接入控制
2. 标准化
3. 扩展引入zbus的多语言跨平台能力

总线架构的一个核心需求是提供便捷的服务适配能力，zbus通过MQ和RPC来完成，对

1. 新服务 -- MQ-RPC模式完成，无侵入式
2. 旧服务 -- 选择旧服务支持的平台接入，通过MQ消息代理模式完成协议转换

新服务接入参考zbus实现RPC部分

旧服务MQ代理模式适配数据流描述：


	zbus标准RPC客户端 <----> zbus（某个MQ队列）------->consumer线程消费消息----RPC消息包解包---->旧协议组装调用旧服务
	                              ^                                                          |
	                              |                                                          v
	                              --------------consumer.route命令返回<-----组装RPC消息包<----旧服务返回结果


代理模式一般在调用旧服务的时候采用异步模式，防止同步阻塞的场景发生
标准化RPC则采用zbus的JSON协议方式序列化消息与zbus消息交换，当然也可以私有的方式。

下面的子项目是多个语言平台实现MQ代理的案例

* [微软MSMQ|国信交易调度](http://git.oschina.net/rushmore/zbus-proxy-msmq "zbus-proxy-msmq") 
* [金证KCXP](http://git.oschina.net/rushmore/zbus-proxy-kcxp "zbus-proxy-kcxp") 
* [国信TC](http://git.oschina.net/rushmore/zbus-proxy-tc "zbus-proxy-tc") 
* [桥接JAVA客户端](http://git.oschina.net/rushmore/zbus-proxy-java "zbus-proxy-java") 
* [国泰君安GTA](http://git.oschina.net/rushmore/zbus-proxy-gta "zbus-proxy-gta")




##zbus底层编程扩展

接入zbus只需要遵循公开协议即可，目前已经支持的接入平台包括
* [Java API](http://git.oschina.net/rushmore/zbus "zbus") 
* [C/C++ API](http://git.oschina.net/rushmore/zbus-api-c "zbus-api-c") 
* [Python API](http://git.oschina.net/rushmore/zbus-api-python "zbus-api-python") 
* [C# API](http://git.oschina.net/rushmore/zbus-api-csharp "zbus-api-csharp") 
* [Node.JS API](http://git.oschina.net/rushmore/zbus-api-nodejs "zbus-api-nodejs") 


###zbus协议说明
zbus协议可以简单描述为扩展HTTP协议，协议整体格式是HTTP格式，因为HTTP协议的广泛应用，相对方便解释与理解。但同时为了降低HTTP协议头部负载与业务数据独立于zbus控制数据，zbus采用了HTTP扩展协议：
* 控制数据放在HTTP扩展头部，比如增加mq: MyMQ\r\n扩展控制消息目标MQ
* 业务数据放在HTTP消息体，不参与任何zbus消息控制，业务数据底层为byte[]二进制

因此zbus协议描述就是HTTP扩展的KeyValue描述


命令控制 cmd

zbus接收到消息Message做何种动作，由cmd KV扩展决定，支持的赋值（Protocol.java 中定义）
	
	public static final String Produce   = "produce";   //生产消息命令
	public static final String Consume   = "consume";   //消费消息命令
	public static final String Route     = "route";     //路由回发送者命令
	public static final String QueryMQ   = "query_mq";  //查询消息队列信息
	public static final String CreateMQ  = "create_mq"; //创建消息队列
	public static final String RemoveMQ  = "remove_mq"; //删除消息队列 
	public static final String AddKey    = "add_key";   //增加一个Key，用于判定某条消息是否重复，zbus简单的KV服务
	public static final String RemoveKey = "remove_key";//删除一个Key 
	//下面的命令是监控中使用到，test测试，data返回监控数据，jquery监控使用到的jquery.js
	public static final String Auth      = "auth";  
	public static final String Test      = "test";      
	public static final String Data      = "data"; 
	public static final String Jquery    = "jquery"; 

每个命令可能用到参数Key说明（Message.java）

	
	public static final String MQ       = "mq";      //消息队列标识 
	public static final String SENDER   = "sender";  //消息发送者标识
	public static final String RECVER   = "recver";  //消息接收者标识
	public static final String ID       = "id";	 //消息ID
	public static final String RAWID    = "rawid";   //原始消息ID（消费消息时交换中用到）
	public static final String SERVER   = "server";  //消息经过的broker地址
	public static final String TOPIC    = "topic";   //消息发布订阅主题， 使用,分隔 
	public static final String ACK      = "ack";	  //消息ACK
	public static final String ENCODING = "encoding"; //消息body二进制编码
	public static final String KEY       = "key";      //消息的KEY
	public static final String KEY_GROUP = "key_group"; //消息的KEY分组
	public static final String MASTER_MQ  = "master_mq";   //消息队列主从复制的主队列标识
	public static final String MASTER_TOKEN  = "master_token";  //主队列访问控制码


具体每个命令对应使用到的参数，请参考MqAdaptor中对应每个命令的Handler


	public class MqAdaptor extends IoAdaptor implements Closeable {
		public MqAdaptor(MqServer mqServer){ 
			....
			registerHandler(Protocol.Produce, produceHandler); 
			registerHandler(Protocol.Consume, consumeHandler);  
			registerHandler(Protocol.Route, routeHandler); 
			
			registerHandler(Protocol.CreateMQ, createMqHandler);
			registerHandler(Protocol.QueryMQ, queryMqHandler);
			registerHandler(Protocol.RemoveMQ, removeMqHandler);
			
			registerHandler(Protocol.AddKey, addKeyHandler); 
			registerHandler(Protocol.RemoveKey, removeKeyHandler); 
			 
			registerHandler("", homeHandler);  
			registerHandler(Protocol.Data, dataHandler); 
			registerHandler(Protocol.Jquery, jqueryHandler);
			registerHandler(Protocol.Test, testHandler);
			
			registerHandler(Message.HEARTBEAT, heartbeatHandler);   
		} 
	}

###zbus网络编程模型

zbus底层通信基础并没有采用netty这样的NIO框架，而是基于JAVA NIO做了一个简单的封装，尽管没有使用到netty的大量开箱即用的功能，但是zbus也在通信基础上获取了些我们认为更加重要的东西：
1. 完全自主个性化的网络事件模型
2. 轻量级通信底层

zbus的网络通讯部分核心在org.zbus.net.core包中，org.zbus.net.http 提供了一个轻量级的HTTP扩展实现。

zbus的NIO通信模型的封装非常简单：

	1. 网络事件模型是由SelectorThread来完成，核心就是run方法中的多路复用检测网络IO事件
	2. 在各个事件处理中（READ/WRITE/CONNECT/ACCEPT)中核心产生了Session处理
	3. 事件处理公开机制靠IoAdaptor完成
	4. 最外面由SelectorGroup完成多个SelectorThread的负载均衡与简单管理，提高整体性能

上面的描述也是解读代码的先后顺序

zbus在net.core包设计的基础之上，为了方便使用方构建客户端与服务器端程序，提供了Client、Server的基本封装，同步异步处理Sync方便消息的同步异步转换。

Client本质上就一个IoAdaptor应用案例，专门从连接客户端角度处理网络各项事件。
Server则提供了一个简单机制，运行可被个性化的IoAdaptor实例。

Server端示例（简洁性的体现）

	//借助HTTP协议实现中的MessageAdaptor完成HTTP服务器，只需要简单的
	public static void main(String[] args) throws Exception {
		//1） SelectorGroup管理 
		final SelectorGroup group = new SelectorGroup();
		final Server server = new Server(group);
		//2)构建一个MessageAdaptor
		MessageAdaptor ioAdaptor = new MessageAdaptor();
		ioAdaptor.uri("/hello", new MessageProcessor() { 
			public Message process(Message request) {
				Message resp = new Message();
				resp.setStatus(200);
				resp.setBody("hello");
				return resp;
			}
		});
		//3)在8080端口上启动这个IoAdaptor服务
		server.start(8080, ioAdaptor);
	}

运行则直接可以统统浏览器访问 http://localhost:8080/hello

这个示例并不是简单的hello world，SelectorGroup使之具备高性能服务框架，在i7 CPU的box上能上10w+的QPS性能

具体请详细参考examples下面的net示例




##zbus高可用模式

zbus高可用采用类似zookeeper的跟踪机制，但并没有使用zookeeper。

zbus高可用由两大节点群组成：
1. ZbusServer节点群
2. TrackServer节点群

ZbusServer节点群由单机版本的zbus组成，各个节点之间无状态关联，TrackServer节点群中各个节点也无任何状态关联，ZbusServer把节点状态（诸如MQ信息）上报给所有的TrackServer。

这里面信息的一致性zbus是做了妥协的，可以理解为zookeeper一种简化，典型配置是TrackServer全网配置两台，所有的ZbusServer都向这两台TrackServer上报各自的节点变化信息，包括某个节点MQ信息即时变化推送，定时（默认2s）重复更新。实用角度简化设计的一种折中。

客户端（生产者与消费者）仍然是直接链接到某个ZbusServer，但是选择节点由订阅TrackServer而给出的整体ZbusServer的节点拓扑信息决定，客户端同时做了容错处理，运行中所有的TrackServer失败不影响已有拓扑信息的实用（本地缓存）

上述客户端的复杂性由HaBroker封装（最终由ZbusBroker统一类型选择），API层面不受高可用选择影响与单点zbus场景保持一致，同时高可用节点选择算法也将陆续开放，方便二次开发个性化。

高可用HA环境的搭建
因为zbus HA方案中的节点无任何状态联系，因此HA环境搭建非常简单，各个节点启动无顺序依赖，一般顺序为：

1. 启动若干个（一般两个）TrackServer群。
2. 配置预先知道的TrackServer列表到ZbusServer（MqServer）的启动项中，启动若干个ZbusServer节点群。

整个HA的Broker环境就建立好了，例子可以参考zbus-dist/ha 目录下的配置启动，注意如果不按照先TrackServer启动的顺序，先启动ZbusServer会临时报无法找到某个TrackServer错误，直到TrackServer启动正常，错误不影响使用。


HA最佳实践指导：

1. 生产者端（包括RPC客户端）配置HaBroker，完全实现Failover与负载均衡
2. 消费者端（包括RPC服务端）配置SingleBroker，不使用HaBroker的容错，而是多机部署，这样能运行确定节点分布。

上述HaBroker与SingleBroker都统一使用ZbusBroker，只是BrokerAddress的地址配置差异。


##zbus性能测试数据

性能测试程序在test/performance目录下，根据实际的机器测试给出。

一个参考数据，测试环境

	MacBook Pro (Retina, 15-inch, Mid 2015)
	Processor 2.5 GHz Intel Core i7
	Memory 16 GB 1600 MHz DDR3

消息大小 "hello world"

* 生产消息速度 （~4万笔每秒）

测试代码： org.zbus.performance.ProducerPerf.java
	
	2016-03-16 14:50:08 INFO  Perf:73 - QPS: 42844.4874, Failed/Total=0/2660020(0.0000)
	2016-03-16 14:50:09 INFO  Perf:73 - QPS: 42845.4515, Failed/Total=0/2670011(0.0000)
	2016-03-16 14:50:09 INFO  Perf:73 - QPS: 42842.2988, Failed/Total=0/2680012(0.0000)
	2016-03-16 14:50:09 INFO  Perf:73 - QPS: 42841.8991, Failed/Total=0/2690011(0.0000)
	2016-03-16 14:50:09 INFO  Perf:73 - QPS: 42838.7834, Failed/Total=0/2700020(0.0000)
	2016-03-16 14:50:10 INFO  Perf:73 - QPS: 42840.4312, Failed/Total=0/2710016(0.0000)
	2016-03-16 14:50:10 INFO  Perf:73 - QPS: 42840.0428, Failed/Total=0/2720007(0.0000)


* 消费消息速度（~4万笔每秒）

测试代码： org.zbus.performance.ConsumerPerf.java

	2016-03-16 14:57:13 INFO  ConsumerPerf:47 - Consumed:150000, QPS: 43290.0433
	2016-03-16 14:57:13 INFO  ConsumerPerf:47 - Consumed:160000, QPS: 43859.6491
	2016-03-16 14:57:14 INFO  ConsumerPerf:47 - Consumed:170000, QPS: 43859.6491
	2016-03-16 14:57:14 INFO  ConsumerPerf:47 - Consumed:180000, QPS: 43290.0433
	2016-03-16 14:57:14 INFO  ConsumerPerf:47 - Consumed:190000, QPS: 43859.6491
	2016-03-16 14:57:14 INFO  ConsumerPerf:47 - Consumed:200000, QPS: 44247.7876
	2016-03-16 14:57:15 INFO  ConsumerPerf:47 - Consumed:210000, QPS: 43668.1223
	2016-03-16 14:57:15 INFO  ConsumerPerf:47 - Consumed:220000, QPS: 44843.0493
	2016-03-16 14:57:15 INFO  ConsumerPerf:47 - Consumed:230000, QPS: 44843.0493



* HTTP响应速度（~6.6万笔每秒）

测试代码，服务器: org.zbus.examples.net.server.MyServer

压力程序: ab -k -c 20 -n 1000000 http://localhost:8080/hello

	
	Server Hostname:        localhost
	Server Port:            8080
	
	Document Path:          /hello
	Document Length:        5 bytes
	
	Concurrency Level:      20
	Time taken for tests:   15.073 seconds
	Complete requests:      1000000
	Failed requests:        0
	Keep-Alive requests:    1000000
	Total transferred:      67000000 bytes
	HTML transferred:       5000000 bytes
	Requests per second:    66344.44 [#/sec] (mean)
	Time per request:       0.301 [ms] (mean)
	Time per request:       0.015 [ms] (mean, across all concurrent requests)
	Transfer rate:          4340.90 [Kbytes/sec] received
	
