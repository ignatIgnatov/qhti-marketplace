-- V15__Add_location2_field.sql
-- Add location2 field to ads table and rename location to location1

-- Step 1: Add the new location2 column
ALTER TABLE ads ADD COLUMN location2 VARCHAR(200);

-- Step 2: Rename existing location to location1
-- (PostgreSQL supports this directly)
ALTER TABLE ads RENAME COLUMN location TO location1;

-- Step 3: Update indexes
-- Drop old location index if it exists
DROP INDEX IF EXISTS idx_ads_location;

-- Create new indexes for both location fields
CREATE INDEX idx_ads_location1 ON ads(location1);
CREATE INDEX idx_ads_location2 ON ads(location2);
CREATE INDEX idx_ads_location1_active_created ON ads(location1, active, created_at DESC);

-- Step 4: Update views that use location
DROP VIEW IF EXISTS boat_ads_with_specs;

CREATE VIEW boat_ads_with_specs AS
SELECT
    a.id,
    a.description,
    a.category,
    a.price_amount,
    a.price_type,
    a.including_vat,
    a.location1,
    a.location2,
    a.ad_type,
    a.user_email,
    a.user_id,
    a.user_first_name,
    a.user_last_name,
    a.created_at,
    a.updated_at,
    a.active,
    a.views_count,
    a.featured,
    a.contact_person_name,
    a.contact_phone,
    a.contact_email,
    a.located_in_bulgaria,
    bs.boat_type,
    bs.brand,
    bs.model,
    bs.engine_type,
    bs.engine_included,
    bs.engine_brand_model,
    bs.horsepower,
    bs.length,
    bs.width,
    bs.draft,
    bs.max_people,
    bs.year,
    bs.in_warranty,
    bs.weight,
    bs.fuel_capacity,
    bs.has_water_tank,
    bs.number_of_engines,
    bs.has_auxiliary_engine,
    bs.fuel_type,
    bs.material,
    bs.is_registered,
    bs.has_commercial_fishing_license,
    bs.condition,
    bs.water_type,
    bs.engine_hours,
    ARRAY_AGG(DISTINCT bp.purpose) FILTER (WHERE bp.purpose IS NOT NULL) as purposes,
    ARRAY_AGG(DISTINCT bct.console_type) FILTER (WHERE bct.console_type IS NOT NULL) as console_types
FROM ads a
LEFT JOIN boat_specifications bs ON a.id = bs.ad_id
LEFT JOIN boat_purposes bp ON bs.id = bp.boat_spec_id
LEFT JOIN boat_console_types bct ON bs.id = bct.boat_spec_id
WHERE a.category = 'BOATS_AND_YACHTS'
GROUP BY a.id, bs.id;

-- Step 5: Add comments
COMMENT ON COLUMN ads.location1 IS 'Primary location field (e.g., city/region)';
COMMENT ON COLUMN ads.location2 IS 'Secondary location field (optional, e.g., district/area)';