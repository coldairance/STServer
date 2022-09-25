for((i=1;i<=$1;i++))
do
	nohup java -jar /home/coldairance/desktop/RPC/STDistributed/Pay/target/Pay-1.0.jar &> /home/coldairance/desktop/test/nohup.out &
	sleep 1s
done
echo "已启动支付服务：${1}"
