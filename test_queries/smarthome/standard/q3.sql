
    SELECT
        platform ,
        dateOnly ,
        sum(power) AS totalpower 
    FROM
        (SELECT
            CONCAT('http://iot.soton.ac.uk/smarthome/platform#',
            sensors.Location) AS platform ,
            HOUR(meter.TimestampUTC) AS hours ,
            CAST(meter.TimestampUTC AS DATE) AS dateOnly ,
            avg(meter.RealPowerWatts) AS power 
        FROM
            sensors ,
            meter 
        WHERE
            meter.MeterName=sensors.SensingDevice 
            AND meter.RealPowerWatts>0 
            AND meter.TimestampUTC>'2012-07-01T00:00:00' 
            AND meter.TimestampUTC<'2012-07-02T00:00:00' 
        GROUP BY
            CAST(meter.TimestampUTC AS DATE) ,
            HOUR(meter.TimestampUTC) ,
            CONCAT('http://iot.soton.ac.uk/smarthome/platform#',
            sensors.Location) ,
            CONCAT('http://iot.soton.ac.uk/smarthome/sensor#',
            sensors.SensingDevice)  )   
    GROUP BY
        dateOnly ,
        platform  