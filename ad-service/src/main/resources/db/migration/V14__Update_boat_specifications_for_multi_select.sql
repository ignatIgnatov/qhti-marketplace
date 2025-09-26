-- V14__Update_boat_specifications_for_multi_select.sql
-- Migration to support multiple boat purposes and console types (FIXED VERSION)

-- =============================================================================
-- CREATE NEW TABLES FOR MULTI-SELECT FIELDS
-- =============================================================================

-- Table for boat purposes (multiple per boat)
CREATE TABLE IF NOT EXISTS boat_purposes (
    id BIGSERIAL PRIMARY KEY,
    boat_spec_id BIGINT NOT NULL REFERENCES boat_specifications(id) ON DELETE CASCADE,
    purpose VARCHAR(50) NOT NULL CHECK (purpose IN (
        'ALL', 'FISHING', 'BEACH', 'WATER_SPORTS', 'WORK', 'OVERNIGHT_STAY'
    )),
    CONSTRAINT unique_purpose_per_boat UNIQUE(boat_spec_id, purpose)
);

-- Table for boat console types (multiple per boat)
CREATE TABLE IF NOT EXISTS boat_console_types (
    id BIGSERIAL PRIMARY KEY,
    boat_spec_id BIGINT NOT NULL REFERENCES boat_specifications(id) ON DELETE CASCADE,
    console_type VARCHAR(50) NOT NULL CHECK (console_type IN (
        'ALL', 'NONE', 'CENTRAL', 'SIDE', 'CABIN', 'FLYBRIDGE'
    )),
    CONSTRAINT unique_console_type_per_boat UNIQUE(boat_spec_id, console_type)
);

-- =============================================================================
-- MIGRATE EXISTING DATA (Simple approach)
-- =============================================================================

-- Migrate existing boat_purpose data to new table (only if new table is empty)
INSERT INTO boat_purposes (boat_spec_id, purpose)
SELECT id, boat_purpose
FROM boat_specifications
WHERE boat_purpose IS NOT NULL
  AND EXISTS (SELECT 1 FROM information_schema.columns
              WHERE table_name='boat_specifications' AND column_name='boat_purpose')
  AND NOT EXISTS (SELECT 1 FROM boat_purposes LIMIT 1);

-- Migrate existing console_type data to new table (only if new table is empty)
INSERT INTO boat_console_types (boat_spec_id, console_type)
SELECT id, console_type
FROM boat_specifications
WHERE console_type IS NOT NULL
  AND EXISTS (SELECT 1 FROM information_schema.columns
              WHERE table_name='boat_specifications' AND column_name='console_type')
  AND NOT EXISTS (SELECT 1 FROM boat_console_types LIMIT 1);

-- =============================================================================
-- UPDATE BOAT_SPECIFICATIONS TABLE
-- =============================================================================

-- Drop the single-value columns (if they exist)
ALTER TABLE boat_specifications DROP COLUMN IF EXISTS boat_purpose;
ALTER TABLE boat_specifications DROP COLUMN IF EXISTS console_type;

-- =============================================================================
-- CREATE INDEXES FOR NEW TABLES
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_boat_purposes_boat_spec_id ON boat_purposes(boat_spec_id);
CREATE INDEX IF NOT EXISTS idx_boat_purposes_purpose ON boat_purposes(purpose);

CREATE INDEX IF NOT EXISTS idx_boat_console_types_boat_spec_id ON boat_console_types(boat_spec_id);
CREATE INDEX IF NOT EXISTS idx_boat_console_types_console_type ON boat_console_types(console_type);

-- =============================================================================
-- UPDATE ENUM CHECK CONSTRAINTS
-- =============================================================================

-- Drop existing fuel type constraint if it exists
ALTER TABLE boat_specifications DROP CONSTRAINT IF EXISTS boat_specifications_fuel_type_check;

-- Add updated constraint with ELECTRIC
ALTER TABLE boat_specifications
    ADD CONSTRAINT boat_specifications_fuel_type_check
    CHECK (fuel_type IS NULL OR fuel_type IN ('ALL', 'PETROL', 'DIESEL', 'LPG', 'HYDROGEN', 'ELECTRIC'));

-- =============================================================================
-- UPDATE VIEWS
-- =============================================================================

-- Drop and recreate the boat_ads_with_specs view to handle the new structure
DROP VIEW IF EXISTS boat_ads_with_specs;

CREATE VIEW boat_ads_with_specs AS
SELECT
    a.id,
    a.description,
    a.category,
    a.price_amount,
    a.price_type,
    a.including_vat,
    a.location,
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
    -- Aggregate purposes and console types as arrays
    ARRAY_AGG(DISTINCT bp.purpose) FILTER (WHERE bp.purpose IS NOT NULL) as purposes,
    ARRAY_AGG(DISTINCT bct.console_type) FILTER (WHERE bct.console_type IS NOT NULL) as console_types
FROM ads a
LEFT JOIN boat_specifications bs ON a.id = bs.ad_id
LEFT JOIN boat_purposes bp ON bs.id = bp.boat_spec_id
LEFT JOIN boat_console_types bct ON bs.id = bct.boat_spec_id
WHERE a.category = 'BOATS_AND_YACHTS'
GROUP BY a.id, bs.id;

-- =============================================================================
-- COMMENTS FOR DOCUMENTATION
-- =============================================================================

COMMENT ON TABLE boat_purposes IS 'Multiple purposes for each boat specification - includes OVERNIGHT_STAY option';
COMMENT ON TABLE boat_console_types IS 'Multiple console types for each boat specification';
COMMENT ON COLUMN boat_purposes.purpose IS 'Purpose type: ALL, FISHING, BEACH, WATER_SPORTS, WORK, OVERNIGHT_STAY';
COMMENT ON COLUMN boat_console_types.console_type IS 'Console type: ALL, NONE, CENTRAL, SIDE, CABIN, FLYBRIDGE';