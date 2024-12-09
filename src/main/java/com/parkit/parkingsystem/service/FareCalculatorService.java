package com.parkit.parkingsystem.service;

import java.util.concurrent.TimeUnit;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

	public void calculateFare(Ticket ticket) {
		calculateFare(ticket, false);
	}

    public void calculateFare(Ticket ticket, boolean discount){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        long inHour  = ticket.getInTime().getTime();
        long outHour = ticket.getOutTime().getTime();


        TimeUnit time = TimeUnit.MINUTES;
        long duration = time.convert(outHour - inHour, TimeUnit.MILLISECONDS);

    	double prixVehiculeEnCoursPourUneHeure;

        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
            	prixVehiculeEnCoursPourUneHeure = Fare.CAR_RATE_PER_HOUR;
                break;
            }
            case BIKE: {
            	prixVehiculeEnCoursPourUneHeure = Fare.BIKE_RATE_PER_HOUR;
                break;
            }

            default: throw new IllegalArgumentException("Unkown Parking Type");
        }
        double prix;
        if (duration <= 30) { // le véhicule reste moins de 30 minutes
        	prix = (prixVehiculeEnCoursPourUneHeure * 0);
        }else if (duration <= 45) { // le véhicule reste moins d'une heure
        	prix = (prixVehiculeEnCoursPourUneHeure * 0.75);
        }else { // le véhicule reste plus d'une heure
        	prix = ((duration / 60) * prixVehiculeEnCoursPourUneHeure);
        }

        if (discount) { // le véhicule bénéficie de la réduction fidélité 5%
        	prix = prix * 0.95;
        }

        ticket.setPrice(prix);
    }

}