
    SELECT
        'http://knoesis.wright.edu/ssw/System_4UT01' AS sensor ,
        avg(_4UT01.WindSpeed) AS averageWindSpeed ,
        avg(_4UT01.AirTemperature) AS averageTemperature 
    FROM
        _4UT01 
    WHERE
        _4UT01.time>'2003-04-01T00:00:00' 
        AND _4UT01.time<'2003-04-01T01:00:00' 
        AND _4UT01.AirTemperature>32.0   