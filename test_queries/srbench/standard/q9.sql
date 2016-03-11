
    SELECT
        CASE  
            WHEN avg(_4UT01.WindSpeed) < 1 THEN 0 
            ELSE if(avg(_4UT01.WindSpeed) < 4) 
        END AS windForce ,
        avg(_4UT01.WindDirection) AS avgWindDirection 
    FROM
        _4UT01 
    WHERE
        _4UT01.time>'2003-04-01T00:00:00' 
        AND _4UT01.time<'2003-04-02T00:00:00' 
    GROUP BY
        'http://knoesis.wright.edu/ssw/System_4UT01'  