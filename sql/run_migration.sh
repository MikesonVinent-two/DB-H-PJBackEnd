#!/bin/bash

# 数据库迁移执行脚本
# 作者：AI助手
# 创建日期：2025-06-02

# 设置数据库连接参数
DB_HOST="localhost"
DB_PORT="3306"
DB_NAME="your_database_name"
DB_USER="your_username"
DB_PASS="your_password"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# 显示帮助信息
show_help() {
    echo "数据库迁移执行脚本"
    echo "用法: $0 [options]"
    echo ""
    echo "选项:"
    echo "  -h, --host      数据库主机 (默认: localhost)"
    echo "  -P, --port      数据库端口 (默认: 3306)"
    echo "  -d, --database  数据库名称 (必填)"
    echo "  -u, --user      数据库用户名 (必填)"
    echo "  -p, --password  数据库密码 (必填)"
    echo "  --help          显示此帮助信息"
    echo ""
}

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    key="$1"
    case $key in
        -h|--host)
        DB_HOST="$2"
        shift
        shift
        ;;
        -P|--port)
        DB_PORT="$2"
        shift
        shift
        ;;
        -d|--database)
        DB_NAME="$2"
        shift
        shift
        ;;
        -u|--user)
        DB_USER="$2"
        shift
        shift
        ;;
        -p|--password)
        DB_PASS="$2"
        shift
        shift
        ;;
        --help)
        show_help
        exit 0
        ;;
        *)
        echo -e "${RED}错误: 未知选项 $1${NC}"
        show_help
        exit 1
        ;;
    esac
done

# 检查必填参数
if [ -z "$DB_NAME" ] || [ "$DB_NAME" = "your_database_name" ]; then
    echo -e "${RED}错误: 请指定数据库名称 (-d 或 --database)${NC}"
    show_help
    exit 1
fi

if [ -z "$DB_USER" ] || [ "$DB_USER" = "your_username" ]; then
    echo -e "${RED}错误: 请指定数据库用户名 (-u 或 --user)${NC}"
    show_help
    exit 1
fi

if [ -z "$DB_PASS" ] || [ "$DB_PASS" = "your_password" ]; then
    echo -e "${RED}错误: 请指定数据库密码 (-p 或 --password)${NC}"
    show_help
    exit 1
fi

# 迁移脚本目录
MIGRATIONS_DIR="./migrations"

# 检查迁移脚本目录是否存在
if [ ! -d "$MIGRATIONS_DIR" ]; then
    echo -e "${RED}错误: 迁移脚本目录不存在: $MIGRATIONS_DIR${NC}"
    exit 1
fi

# 获取所有迁移脚本
MIGRATION_FILES=$(find "$MIGRATIONS_DIR" -name "V*__*.sql" | sort)

if [ -z "$MIGRATION_FILES" ]; then
    echo -e "${YELLOW}警告: 没有找到迁移脚本${NC}"
    exit 0
fi

# 执行迁移脚本
echo -e "${GREEN}开始执行数据库迁移...${NC}"
echo "数据库: $DB_NAME"
echo "主机: $DB_HOST:$DB_PORT"
echo "用户: $DB_USER"
echo ""

for file in $MIGRATION_FILES; do
    echo -e "${YELLOW}执行迁移脚本: $(basename "$file")${NC}"
    
    # 执行SQL脚本
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" < "$file"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ 迁移成功${NC}"
    else
        echo -e "${RED}✗ 迁移失败${NC}"
        exit 1
    fi
    
    echo ""
done

echo -e "${GREEN}所有迁移脚本执行完成!${NC}" 