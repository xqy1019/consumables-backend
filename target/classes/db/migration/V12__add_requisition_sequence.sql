-- 申领单序列号生成序列（替换内存 AtomicInteger，支持多实例部署和重启）
CREATE SEQUENCE IF NOT EXISTS requisition_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    CACHE 1;
