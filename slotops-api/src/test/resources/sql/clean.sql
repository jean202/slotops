-- Shared test cleanup: delete all tables in FK-safe order (children first).
-- Applied via @Sql before each controller test method. When a new entity/table
-- with foreign keys is added, update ONLY this file.
delete from notification_logs;
delete from staff_notes;
delete from reservation_addons;
delete from payments;
delete from reservations;
delete from addons;
delete from resource_images;
delete from availability_rules;
delete from blackout_periods;
delete from resources;
delete from customers;
delete from locations;
delete from organization_members;
delete from organizations;
delete from users;
