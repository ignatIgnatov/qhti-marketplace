-- V11__Remove_title_and_quick_description.sql
-- Migration to remove title and quick_description from ads

-- 1. Drop dependent views
DROP VIEW IF EXISTS active_ads;
DROP VIEW IF EXISTS featured_ads;
DROP VIEW IF EXISTS boat_ads_with_specs;
DROP VIEW IF EXISTS recent_ads;

-- 2. Drop dependent index (was using title)
DROP INDEX IF EXISTS idx_ads_text_search;

-- 3. Drop the columns from ads
ALTER TABLE ads
    DROP COLUMN IF EXISTS title,
    DROP COLUMN IF EXISTS quick_description;

-- 4. Recreate index (now only on description)
CREATE INDEX idx_ads_text_search
    ON ads USING gin(to_tsvector('english', description));

-- 5. Recreate the views without title / quick_description
CREATE OR REPLACE VIEW active_ads AS
SELECT * FROM ads
WHERE active = true AND archived = false;

CREATE OR REPLACE VIEW featured_ads AS
SELECT * FROM ads
WHERE active = true AND featured = true AND archived = false;

CREATE OR REPLACE VIEW boat_ads_with_specs AS
SELECT
    a.*,
    bs.boat_type,
    bs.brand,
    bs.model,
    bs.horsepower,
    bs.length,
    bs.year,
    bs.boat_purpose,
    bs.water_type,
    bs.engine_hours,
    bs.located_in_bulgaria
FROM ads a
JOIN boat_specifications bs ON a.id = bs.ad_id
WHERE a.active = true AND a.category = 'BOATS_AND_YACHTS';

CREATE OR REPLACE VIEW recent_ads AS
SELECT * FROM ads
WHERE active = true
ORDER BY created_at DESC
LIMIT 1000;
