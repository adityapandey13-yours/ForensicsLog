-- Seed 16 IOC detection rules (idempotent via ON CONFLICT)
INSERT INTO ioc_rules (name, description, pattern, severity, category, enabled) VALUES 
('AUTH_FAILURE_BURST', 'Detects authentication failure bursts', 'authentication\s+fail', 'HIGH', 'Brute Force', true) ON CONFLICT (name) DO NOTHING;

INSERT INTO ioc_rules (name, description, pattern, severity, category, enabled) VALUES 
('PRIVILEGE_ESCALATION', 'Detects sudo failures', '(?=.*sudo)(?=.*fail)', 'CRITICAL', 'Privilege Escalation', true) ON CONFLICT (name) DO NOTHING;

INSERT INTO ioc_rules (name, description, pattern, severity, category, enabled) VALUES 
('CVE_PATTERN', 'Detects CVE references in logs', 'CVE-\d{4}-\d{4,7}', 'CRITICAL', 'Vulnerability', true) ON CONFLICT (name) DO NOTHING;

INSERT INTO ioc_rules (name, description, pattern, severity, category, enabled) VALUES 
('SUSPICIOUS_PROCESS', 'Detects suspicious netcat usages', '\b(nc|netcat|ncat)\b', 'HIGH', 'Reverse Shell', true) ON CONFLICT (name) DO NOTHING;

INSERT INTO ioc_rules (name, description, pattern, severity, category, enabled) VALUES 
('ACCESS_DENIED', 'Detects general access denied', '(access|permission)\s+denied', 'MEDIUM', 'Unauthorized Access', true) ON CONFLICT (name) DO NOTHING;

INSERT INTO ioc_rules (name, description, pattern, severity, category, enabled) VALUES 
('SUID_SGID_EXECUTION', 'Detects suspicious suid/sgid usage', '\b(suid|sgid|setuid|setgid|chmod\s+[2467]\d{3})\b', 'CRITICAL', 'Privilege Escalation', true) ON CONFLICT (name) DO NOTHING;

INSERT INTO ioc_rules (name, description, pattern, severity, category, enabled) VALUES 
('REVERSE_SHELL', 'Detects reverse shell payloads', '\b(bash\s+-i|/dev/tcp|mkfifo|mknod)\b', 'CRITICAL', 'Reverse Shell', true) ON CONFLICT (name) DO NOTHING;

INSERT INTO ioc_rules (name, description, pattern, severity, category, enabled) VALUES 
('PORT_SCAN', 'Detects common port scanners', '\b(nmap|masscan|port\s+scan)\b', 'HIGH', 'Reconnaissance', true) ON CONFLICT (name) DO NOTHING;

INSERT INTO ioc_rules (name, description, pattern, severity, category, enabled) VALUES 
('SQL_INJECTION', 'Detects SQL Injection attempts', '(union\s+select|''\s*or\s+''1''\s*=\s*''1|drop\s+table)', 'HIGH', 'Web Attack', true) ON CONFLICT (name) DO NOTHING;

INSERT INTO ioc_rules (name, description, pattern, severity, category, enabled) VALUES 
('PATH_TRAVERSAL', 'Detects path traversal attempts', '\.\./\.\./', 'MEDIUM', 'Web Attack', true) ON CONFLICT (name) DO NOTHING;

INSERT INTO ioc_rules (name, description, pattern, severity, category, enabled) VALUES 
('CRYPTO_MINER', 'Detects cryptocurrency miners', '\b(crypto[_-]?miner|xmrig|coinhive|minerd)\b', 'HIGH', 'Cryptojacking', true) ON CONFLICT (name) DO NOTHING;

INSERT INTO ioc_rules (name, description, pattern, severity, category, enabled) VALUES 
('DATA_EXFILTRATION', 'Detects potential data exfiltration', '\b(exfiltrat|large\s+transfer|data\s+leak)\b', 'CRITICAL', 'Data Theft', true) ON CONFLICT (name) DO NOTHING;

INSERT INTO ioc_rules (name, description, pattern, severity, category, enabled) VALUES 
('ROOTKIT_INDICATOR', 'Detects potential rootkit behavior', '\b(rootkit|hidden\s+process|ld_preload)\b', 'CRITICAL', 'Rootkit', true) ON CONFLICT (name) DO NOTHING;

INSERT INTO ioc_rules (name, description, pattern, severity, category, enabled) VALUES 
('SUSPICIOUS_CRON', 'Detects suspicious cron jobs pulling files from the internet', 'cron.*(wget|curl|python|perl|bash).*http', 'HIGH', 'Persistence', true) ON CONFLICT (name) DO NOTHING;

INSERT INTO ioc_rules (name, description, pattern, severity, category, enabled) VALUES 
('FIREWALL_BYPASS', 'Detects attempts to disable firewall', '(firewall.*bypass|iptables\s+-F|ufw\s+disable)', 'CRITICAL', 'Defense Evasion', true) ON CONFLICT (name) DO NOTHING;

INSERT INTO ioc_rules (name, description, pattern, severity, category, enabled) VALUES 
('SSH_KEY_INJECTION', 'Detects unauthorized ssh key addition', 'authorized_keys.*(echo|cat|>>)', 'CRITICAL', 'Persistence', true) ON CONFLICT (name) DO NOTHING;
