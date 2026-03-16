-- Insert mock data for customers
INSERT INTO customers (customer_id, customer_name, national_id)
VALUES
    ('10000001', '张三', '110101199003077758'),
    ('10000002', '李四', '110101198805123456'),
    ('10000003', '王五', '110101197712345678');

-- Insert mock data for managers
INSERT INTO managers (manager_id, manager_name)
VALUES
    ('200001', '李经理'),
    ('200002', '王经理'),
    ('200003', '赵经理');

-- Insert mock data for reports
INSERT INTO reports (customer_id, manager_id, service_code, current_plan, additional_services, status)
VALUES
    (1, 1, 'FTTH_500M', '畅享融合 199 套餐', '天翼云盘,天翼高清', 'completed'),
    (2, 2, 'FTTH_1000M', '畅享融合 299 套餐', '天翼云盘', 'processing'),
    (3, 3, 'FTTH_200M', '畅享融合 99 套餐', NULL, 'pending');

-- Insert mock data for spending history
INSERT INTO spending_history (report_id, month, amount)
VALUES
    (1, '2025-10-01', 199.00),
    (1, '2025-11-01', 199.00),
    (1, '2025-12-01', 210.00),
    (2, '2025-10-01', 299.00),
    (2, '2025-11-01', 299.00),
    (3, '2025-10-01', 99.00);

-- Insert mock data for complaints
INSERT INTO complaints (report_id, complaint_date, description)
VALUES
    (1, '2025-12-15', '宽带网速慢'),
    (2, '2025-11-20', '客服响应时间过长');

-- Insert mock data for network quality
INSERT INTO network_quality (report_id, metric, value)
VALUES
    (1, '丢包率', '0.1%'),
    (1, '延迟', '5ms'),
    (2, '丢包率', '0.5%'),
    (2, '延迟', '10ms');