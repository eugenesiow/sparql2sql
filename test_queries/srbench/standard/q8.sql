
    SELECT
        min(_4UT01.AirTemperature) AS minTemperature ,
        max(_4UT01.AirTemperature) AS maxTemperature 
    FROM
        _4UT01 
    WHERE
        _4UT01.time>'2003-04-01T00:00:00' 
        AND _4UT01.time<'2003-04-02T00:00:00'   