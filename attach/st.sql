/*
 Navicat Premium Data Transfer

 Source Server         : tengxunyun
 Source Server Type    : MySQL
 Source Server Version : 50738
 Source Host           : 101.42.164.61:3306
 Source Schema         : st

 Target Server Type    : MySQL
 Target Server Version : 50738
 File Encoding         : 65001

 Date: 22/05/2022 12:51:20
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for st_good
-- ----------------------------
DROP TABLE IF EXISTS `st_good`;
CREATE TABLE `st_good` (
  `gid` int(11) NOT NULL COMMENT '商品id',
  `name` varchar(20) DEFAULT NULL COMMENT '商品名',
  `price` varchar(255) DEFAULT NULL COMMENT '商品价格',
  `number` int(11) DEFAULT NULL COMMENT '商品库存',
  `description` varchar(255) DEFAULT NULL COMMENT '商品描述',
  `cut` varchar(255) DEFAULT NULL COMMENT '简要图',
  `full` varchar(255) DEFAULT NULL COMMENT '全图',
  PRIMARY KEY (`gid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of st_good
-- ----------------------------
BEGIN;
INSERT INTO `st_good` VALUES (1, '浅濑神社', '19.99', 1147, '位于清籁岛附近的小岛上，由于长年战争已没有什么客人，据说宫司是一只黑猫。', 'cut/qinlai.png', 'nomark/qinlai.png');
INSERT INTO `st_good` VALUES (2, '龙脊雪山', '24.99', 1147, '本是蒙德境内唯一的苍翠乐土，却在天灾之后变为终年冰雪不化的雪山，据说藏匿着天空岛的秘密，是冒险家们的最爱。', 'cut/longji.png', 'nomark/longji.png');
INSERT INTO `st_good` VALUES (3, '绝云间', '49.99', 1147, '云中有如亭阁般耸立的山峰。纵使知晓此间绝非凡人应当涉足之地，这样的华光雾海也会有如漩涡一般，吸引着憧憬天空、仙居之人吧。', 'cut/jueyun.png', 'nomark/jueyun.png');
INSERT INTO `st_good` VALUES (4, '风龙废墟', '24.99', 1147, '独据高塔者已不再。盘旋废都的风低吟着无人无人可知之事：它过去的主人，风中无名的精灵，以及撼动尖塔的群唱...', 'cut/fenglong.png', 'nomark/fenglong.png');
INSERT INTO `st_good` VALUES (5, '层岩巨渊', '39.99', 1147, '隶属于璃月，整个地区形似于漩涡，通向不知名的深渊。', 'cut/cengyan.png', 'nomark/cengyan.png');
INSERT INTO `st_good` VALUES (6, '鸣神大社', '29.99', 1147, '鸣神大社坐落于影向山前，守护着独一无二的神樱，是稻妻最大的神社。在并不太平的今日，为稻妻的民众提供可贵的慰藉与安宁。', 'cut/minshen.png', 'nomark/minshen.png');
COMMIT;

-- ----------------------------
-- Table structure for st_order
-- ----------------------------
DROP TABLE IF EXISTS `st_order`;
CREATE TABLE `st_order` (
  `oid` int(255) NOT NULL AUTO_INCREMENT COMMENT '订单id',
  `uid` int(10) unsigned DEFAULT NULL COMMENT '用户id',
  `gid` int(11) DEFAULT NULL COMMENT '商品id',
  `number` int(11) DEFAULT NULL COMMENT '商品数量',
  `discount` varchar(255) DEFAULT NULL COMMENT '折扣',
  PRIMARY KEY (`oid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of st_order
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for st_receipt
-- ----------------------------
DROP TABLE IF EXISTS `st_receipt`;
CREATE TABLE `st_receipt` (
  `rid` int(255) NOT NULL AUTO_INCREMENT COMMENT '收据id',
  `uid` int(10) unsigned DEFAULT NULL COMMENT '用户id',
  `oid` int(255) DEFAULT NULL COMMENT '订单id',
  `money` varchar(255) DEFAULT NULL COMMENT '结算金额',
  PRIMARY KEY (`rid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of st_receipt
-- ----------------------------
BEGIN;
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
