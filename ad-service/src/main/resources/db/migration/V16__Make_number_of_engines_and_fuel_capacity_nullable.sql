-- V16__Make_number_of_engines_and_fuel_capacity_nullable.sql
-- Remove NOT NULL constraints from number_of_engines and fuel_capacity

-- Step 1: Remove NOT NULL constraint from number_of_engines
ALTER TABLE boat_specifications
ALTER COLUMN number_of_engines DROP NOT NULL;

-- Step 2: Remove NOT NULL constraint from fuel_capacity
ALTER TABLE boat_specifications
ALTER COLUMN fuel_capacity DROP NOT NULL;

-- Step 3: Update comments to reflect that fields are now optional
COMMENT ON COLUMN boat_specifications.number_of_engines IS 'Number of engines (optional, 0-10 if specified)';
COMMENT ON COLUMN boat_specifications.fuel_capacity IS 'Fuel capacity in liters (optional, >= 0 if specified)';