-- Only inserts if tables are empty (safe for repeated restarts)
INSERT INTO blueprints (author, name)
SELECT 'john', 'house'
WHERE NOT EXISTS (SELECT 1 FROM blueprints WHERE author='john' AND name='house');

INSERT INTO blueprints (author, name)
SELECT 'john', 'garage'
WHERE NOT EXISTS (SELECT 1 FROM blueprints WHERE author='john' AND name='garage');

INSERT INTO blueprints (author, name)
SELECT 'jane', 'garden'
WHERE NOT EXISTS (SELECT 1 FROM blueprints WHERE author='jane' AND name='garden');

-- Points for john/house
INSERT INTO points (blueprint_id, x, y, position)
SELECT b.id, 0,  0,  0 FROM blueprints b WHERE b.author='john' AND b.name='house'
  AND NOT EXISTS (SELECT 1 FROM points p WHERE p.blueprint_id=b.id);

INSERT INTO points (blueprint_id, x, y, position)
SELECT b.id, 10, 0,  1 FROM blueprints b WHERE b.author='john' AND b.name='house'
  AND (SELECT COUNT(*) FROM points p WHERE p.blueprint_id=b.id) < 2;

INSERT INTO points (blueprint_id, x, y, position)
SELECT b.id, 10, 10, 2 FROM blueprints b WHERE b.author='john' AND b.name='house'
  AND (SELECT COUNT(*) FROM points p WHERE p.blueprint_id=b.id) < 3;

INSERT INTO points (blueprint_id, x, y, position)
SELECT b.id, 0,  10, 3 FROM blueprints b WHERE b.author='john' AND b.name='house'
  AND (SELECT COUNT(*) FROM points p WHERE p.blueprint_id=b.id) < 4;