-- CREATE TABLE Location (
--   ItemID INT,
--   Position POINT NOT NULL

-- )
-- ENGINE = MYISAM;

-- INSERT INTO Location(ItemID, Position)
-- SELECT ItemID, Point(Latitude, Longitude) FROM Item WHERE Latitude IS NOT NULL AND Longitude IS NOT NULL;

-- CREATE SPATIAL INDEX sp_index ON Location(Position);


CREATE TABLE Location (
  ItemID INT,
  Position POINT NOT NULL

) ENGINE = MyISAM;

INSERT INTO Location (ItemID, Position)
SELECT ItemID, POINT(Latitude, Longitude) FROM Item WHERE Latitude<>'null' AND Longitude<>'null';

CREATE SPATIAL INDEX sp_index ON Location (Position);