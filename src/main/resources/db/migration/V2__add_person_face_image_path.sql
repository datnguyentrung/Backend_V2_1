ALTER TABLE core.person
    ADD COLUMN IF NOT EXISTS face_image_path VARCHAR(500);
