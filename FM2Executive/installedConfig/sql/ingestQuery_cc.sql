
select * from imageextent where extent_name = 'christchurch';

select extent_id, count(*) from fmimage where extent_id = 2;

select metadata_field_id, count(*), metadata_value from imagemetadata where metadata_field_id = 1 and image_id in (select image_id from fmimage where extent_id=2) group by metadata_value;

select metadata_field_id, count(*), metadata_value from imagemetadata where metadata_field_id = 2 and image_id in (select image_id from fmimage where extent_id=2) group by metadata_value;

