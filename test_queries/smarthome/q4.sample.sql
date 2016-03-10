
    SELECT
        motiondate ,
        motionhours ,
        motionplatform ,
        power ,
        meter ,
        name 
    FROM
        (SELECT
            CONCAT('http://iot.soton.ac.uk/smarthome/platform#',
            sensors.Location) AS meterplatform ,
            HOUR(meter.TimestampUTC) AS meterhours ,
            CAST(meter.TimestampUTC AS DATE) AS meterdate ,
            avg(meter.RealPowerWatts) AS power ,
            CONCAT('http://iot.soton.ac.uk/smarthome/sensor#',
            sensors.SensingDevice) AS meter ,
            MAX(sensors.Label) AS name 
        FROM
            sensors ,
            meter 
        WHERE
            meter.MeterName=sensors.SensingDevice 
            AND meter.RealPowerWatts>0 
            AND meter.TimestampUTC>'2012-07-01T00:00:00' 
            AND meter.TimestampUTC<'2012-07-07T00:00:00' 
        GROUP BY
            CONCAT('http://iot.soton.ac.uk/smarthome/platform#',
            sensors.Location) ,
            HOUR(meter.TimestampUTC) ,
            CAST(meter.TimestampUTC AS DATE) ,
            CONCAT('http://iot.soton.ac.uk/smarthome/sensor#',
            sensors.SensingDevice)  )  ,
        (SELECT
            CONCAT('http://iot.soton.ac.uk/smarthome/platform#',
            sensors.Location) AS motionplatform ,
            HOUR(motion.TimestampUTC) AS motionhours ,
            CAST(motion.TimestampUTC AS DATE) AS motiondate 
        FROM
            sensors ,
            motion 
        WHERE
            sensors.SensingDevice=motion.MotionSensorName 
            AND motion.TimestampUTC>'2012-07-01T00:00:00' 
            AND motion.TimestampUTC<'2012-07-07T00:00:00' 
        GROUP BY
            CONCAT('http://iot.soton.ac.uk/smarthome/platform#',
            sensors.Location) ,
            HOUR(motion.TimestampUTC) ,
            CAST(motion.TimestampUTC AS DATE)  )  
    WHERE
        motionplatform=meterplatform 
        AND motionhours=meterhours 
        AND motiondate=meterdate   