-- Migration script to add 'city' column to all tables
-- Run this script in PostgreSQL to add the required columns

-- Add city column to uhi_heatmap_points
ALTER TABLE uhi_heatmap_points
ADD COLUMN IF NOT EXISTS city VARCHAR(50) DEFAULT 'Ahmedabad' NOT NULL;

-- Add city column to uhi_hotspots
ALTER TABLE uhi_hotspots
ADD COLUMN IF NOT EXISTS city VARCHAR(50) DEFAULT 'Ahmedabad' NOT NULL;

-- Add city column to pollution_points
ALTER TABLE pollution_points
ADD COLUMN IF NOT EXISTS city VARCHAR(50) DEFAULT 'Ahmedabad' NOT NULL;

-- Add city column to pollution_hotspots
ALTER TABLE pollution_hotspots
ADD COLUMN IF NOT EXISTS city VARCHAR(50) DEFAULT 'Ahmedabad' NOT NULL;

-- Add city column to vegetation_points
ALTER TABLE vegetation_points
ADD COLUMN IF NOT EXISTS city VARCHAR(50) DEFAULT 'Ahmedabad' NOT NULL;

-- Add city column to vegetation_alerts
ALTER TABLE vegetation_alerts
ADD COLUMN IF NOT EXISTS city VARCHAR(50) DEFAULT 'Ahmedabad' NOT NULL;

-- Add city column to forecast_steps
ALTER TABLE forecast_steps
ADD COLUMN IF NOT EXISTS city VARCHAR(50) DEFAULT 'Ahmedabad' NOT NULL;

-- Add city column to action_items
ALTER TABLE action_items
ADD COLUMN IF NOT EXISTS city VARCHAR(50) DEFAULT 'Ahmedabad' NOT NULL;

-- Add city column to llm_summaries
ALTER TABLE llm_summaries
ADD COLUMN IF NOT EXISTS city VARCHAR(50) DEFAULT 'Ahmedabad' NOT NULL;

-- Create indexes on city columns for better performance
CREATE INDEX IF NOT EXISTS idx_uhi_city ON uhi_heatmap_points (city);
CREATE INDEX IF NOT EXISTS idx_uhi_hotspot_city ON uhi_hotspots (city);
CREATE INDEX IF NOT EXISTS idx_poll_city ON pollution_points (city);
CREATE INDEX IF NOT EXISTS idx_poll_hotspot_city ON pollution_hotspots (city);
CREATE INDEX IF NOT EXISTS idx_veg_city ON vegetation_points (city);
CREATE INDEX IF NOT EXISTS idx_veg_alert_city ON vegetation_alerts (city);
CREATE INDEX IF NOT EXISTS idx_forecast_city ON forecast_steps (city);
CREATE INDEX IF NOT EXISTS idx_action_city ON action_items (city);
CREATE INDEX IF NOT EXISTS idx_llm_city ON llm_summaries (city);

-- Update existing data to 'Ahmedabad' (already default)
UPDATE uhi_heatmap_points SET city = 'Ahmedabad' WHERE city IS NULL;
UPDATE uhi_hotspots SET city = 'Ahmedabad' WHERE city IS NULL;
UPDATE pollution_points SET city = 'Ahmedabad' WHERE city IS NULL;
UPDATE pollution_hotspots SET city = 'Ahmedabad' WHERE city IS NULL;
UPDATE vegetation_points SET city = 'Ahmedabad' WHERE city IS NULL;
UPDATE vegetation_alerts SET city = 'Ahmedabad' WHERE city IS NULL;
UPDATE forecast_steps SET city = 'Ahmedabad' WHERE city IS NULL;
UPDATE action_items SET city = 'Ahmedabad' WHERE city IS NULL;
UPDATE llm_summaries SET city = 'Ahmedabad' WHERE city IS NULL;

-- Verify columns were added
SELECT
    table_name,
    column_name,
    data_type
FROM information_schema.columns
WHERE column_name = 'city'
AND table_schema = 'public'
ORDER BY table_name;
