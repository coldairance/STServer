for((i=1;i<=$1;i++))
do
	nohup java -jar /home/coldairance/desktop/RPC/STDistributed/Order/target/Order-1.0.jar &> /home/coldairance/desktop/test/nohup.out &
	sleep 1s
done
echo "已启动订单服务：${1}"
	
	
	
