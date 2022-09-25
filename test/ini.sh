nohup java -jar /home/coldairance/desktop/RPC/STDistributed/MysqlProvider/target/MysqlProvider-1.0.jar &> /home/coldairance/desktop/test/nohup.out &
echo "mysql启动"
sleep 1s
nohup java -jar /home/coldairance/desktop/RPC/STDistributed/RedisProvider/target/RedisProvider-1.0.jar &> /home/coldairance/desktop/test/nohup.out &
echo "redis启动"
sleep 1s
nohup java -jar /home/coldairance/desktop/RPC/STDistributed/RabbitProvider/target/RabbitProvider-1.0.jar &> /home/coldairance/desktop/test/nohup.out &
echo "rabbitmq启动"
sleep 1s
nohup java -jar /home/coldairance/desktop/RPC/STDistributed/SocketProvider/target/SocketProvider-1.0.jar &> /home/coldairance/desktop/test/nohup.out &
echo "socket启动"
