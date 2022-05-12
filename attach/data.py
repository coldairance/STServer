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
file = open(f'./data.txt', 'w')
for o in orders:
    file.write(o+'\n')
