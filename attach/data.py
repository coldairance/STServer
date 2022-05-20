import random

orders = []
N = 15000
# 生成 order 订单
for i in range(1, N+1):
    uid = i
    gid = random.randint(1, 6)
    number = random.randint(1, 3)
    s = f'{uid},{gid},{number}'
    orders.append(s)
    # 有人可能会购买其他商品
    if random.random()>0.5:
        uid = random.randint(1, i)
        gid = random.randint(1, 6)
        number = random.randint(1, 3)
        s = f'{uid},{gid},{number}'
        orders.append(s)
    # 频繁访问
    if random.random()>0.8:
        uid = random.randint(1, i)
        for j in range(0,6):
            gid = random.randint(1, 6)
            number = random.randint(1, 3)
            s = f'{uid},{gid},{number}'
            orders.append(s)
# 写入文件
file = open(f'./data.txt', 'w')
for o in orders:
    file.write(o+'\n')
