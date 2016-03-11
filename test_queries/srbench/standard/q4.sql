
    SELECT
        'http://knoesis.wright.edu/ssw/System_4UT01' AS sensor ,
        avg(_4UT01.WindGust) AS averageWindSpeed ,
        avg(_4UT01.DewPoint) AS averageTemperature 
    FROM
        _4UT01 
    WHERE
        _4UT01.time>'2003-04-01T00:00:00' 
        AND _4UT01.time<'2003-04-01T01:00:00' 
        AND _4UT01.DewPoint>32.0 
    GROUP BY
        'http://knoesis.wright.edu/ssw/System_4UT01'  