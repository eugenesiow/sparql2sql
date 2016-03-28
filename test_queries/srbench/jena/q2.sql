
    SELECT
        DISTINCT 'http://knoesis.wright.edu/ssw/System_4UT01' AS sensor ,
        _4UT01.Precipitation AS value ,
        'http://knoesis.wright.edu/ssw/ont/weather.owl#centimeters' AS uom 
    FROM
        _4UT01 
    WHERE
        _4UT01.time>='2003-04-03T16:00:00' 
        AND _4UT01.time<'2003-04-03T17:00:00'   