-- Default SUPER_ADMIN user (email: admin@planify.local, password: password)
-- Change this password immediately in production environments.
INSERT INTO users (name, email, password_hash, platform_role, is_active, created_at, updated_at)
VALUES (
  'System Admin',
  'admin@planify.local',
  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
  'SUPER_ADMIN',
  TRUE,
  UTC_TIMESTAMP(3),
  UTC_TIMESTAMP(3)
);
