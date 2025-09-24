
ALTER TABLE ads 
ADD COLUMN located_in_bulgaria BOOLEAN;

UPDATE ads 
SET located_in_bulgaria = bs.located_in_bulgaria
FROM boat_specifications bs 
WHERE ads.id = bs.ad_id 
  AND ads.category = 'BOATS_AND_YACHTS';

UPDATE ads 
SET located_in_bulgaria = true 
WHERE category != 'BOATS_AND_YACHTS' 
  AND located_in_bulgaria IS NULL;

ALTER TABLE ads 
ALTER COLUMN located_in_bulgaria SET NOT NULL;

DROP VIEW IF EXISTS boat_ads_with_specs;

ALTER TABLE boat_specifications 
DROP COLUMN IF EXISTS located_in_bulgaria;

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
    bs.boat_purpose,
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
    bs.console_type,
    bs.fuel_type,
    bs.material,
    bs.is_registered,
    bs.has_commercial_fishing_license,
    bs.condition,
    bs.water_type,
    bs.engine_hours
FROM ads a
LEFT JOIN boat_specifications bs ON a.id = bs.ad_id
WHERE a.category = 'BOATS_AND_YACHTS';