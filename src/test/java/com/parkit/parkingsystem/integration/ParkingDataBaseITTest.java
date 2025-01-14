package com.parkit.parkingsystem.integration;

import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;

import junit.framework.Assert;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseITTest {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){

    }


    @Test
    public void parkingACarTest(){

    	ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        //TODO: check that a ticket is actualy saved in DB and Parking table is updated with availability
    	boolean resultatAvantLancementProcessIncomingVehicle = verifierDisponibiliteParking();
		Assert.assertTrue("Le parking doit être vide", resultatAvantLancementProcessIncomingVehicle);
		// on vérifie avant l'appel processIncomingVehicle que la place de parking en base est bien vide

	    // lancement metier
		parkingService.processIncomingVehicle();
		int resultatTicket = verifierTicketAffecteAuBonParking();
		boolean resultatApresLancementProcessIncomingVehicle = verifierDisponibiliteParking();
		Assert.assertEquals("le numéro de place de parking attendu est le 1", resultatTicket, 1);
		Assert.assertFalse("Le parking doit être occupé", resultatApresLancementProcessIncomingVehicle);

    }
    @Test
    public void parkingLotExitTest(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingACarTest();
        try {
			TimeUnit.SECONDS.sleep(2);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        Date dateAvantLancementMethode = DateUtils.addSeconds(new Date(), -2);

        parkingService.processExitingVehicle();
        Ticket ticket = recupererTicket ();
        Assert.assertEquals("le prix doit être alimenté et égal à", 0.0, ticket.getPrice());
        Date outTimeTicket = ticket.getOutTime();
        //Date dateApresLancementMethode = new Date();
        Date dateApresLancementMethode = DateUtils.addSeconds(new Date(), 2);

        if (dateAvantLancementMethode == null) {
        	System.out.println("dateAvantLancementMethode == null");
        }
        if (dateApresLancementMethode == null) {
        	System.out.println("dateApresLancementMethode == null");
        }
        if (outTimeTicket == null) {
        	System.out.println("outTimeTicket == null");
        }
        Assert.assertTrue("la plaque d'immatriculation doit être renseignée", ticket.getVehicleRegNumber() != null);
        Assert.assertTrue("ma variable est avant l'heure de sortie du ticket", dateAvantLancementMethode.before(outTimeTicket) || dateAvantLancementMethode.equals(outTimeTicket));
        Assert.assertTrue("ma variable est après l'heure de sortie du ticket", dateApresLancementMethode.after(outTimeTicket) || dateApresLancementMethode.equals(outTimeTicket));
    }

    @Test
    public void parkingLotExitRecurringUserTest () {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        for (int passage = 0; passage < 2; passage ++) {
	    	parkingACarTest();
	        Ticket ticket = recupererTicket();
	        System.out.println("ticketentree"+ticket);
	        	Date heureModifie = DateUtils.addHours(ticket.getInTime(), -2 + passage);
	        miseAJourInTime(heureModifie);
	        parkingService.processExitingVehicle();
	        Ticket ticketSortie = recupererTicket();
	        System.out.println("ticketsortant"+ticketSortie);
       }
        Ticket ticketResultat = recupererTicket();
        Assert.assertEquals("la plaque doit être ABCDEF", "ABCDEF", ticketResultat.getVehicleRegNumber());
        Assert.assertEquals("une réduction du prix doit être appliqué", (Fare.CAR_RATE_PER_HOUR * 0.95), ticketResultat.getPrice());


    }

    public void miseAJourInTime (Date heureAModifier) {
    	Connection con = null;
    	final String MISE_A_JOUR_IN_TIME = "update TICKET set IN_TIME = ? where VEHICLE_REG_NUMBER = 'ABCDEF' AND OUT_TIME is NULL;";
    	try{
    		PreparedStatement ps = dataBaseTestConfig.getConnection().prepareStatement(MISE_A_JOUR_IN_TIME);
			ps.setTimestamp(1, new java.sql.Timestamp(heureAModifier.getTime()));
    		ps.executeUpdate();
			dataBaseTestConfig.closePreparedStatement(ps);

    	}catch (Exception ex) {
    		Assert.fail("aucune exception ne devrait être remontée" + ex.toString());
    	}finally{
    		dataBaseTestConfig.closeConnection(con);
    	}
    }



    public int verifierTicketAffecteAuBonParking () {
    	Connection con = null;
    	int resultat = 0;
    	final String VERIFIER_TICKET_AFFECTE_AU_BON_PARKING = "select PARKING_NUMBER from TICKET where VEHICLE_REG_NUMBER = 'ABCDEF';";
    	try{
    		PreparedStatement verifierTicket = dataBaseTestConfig.getConnection().prepareStatement(VERIFIER_TICKET_AFFECTE_AU_BON_PARKING);
			ResultSet resultatTicket = verifierTicket.executeQuery();
			if(resultatTicket.next()) {
				resultat = resultatTicket.getInt(1);
			}
			dataBaseTestConfig.closeResultSet(resultatTicket);
			dataBaseTestConfig.closePreparedStatement(verifierTicket);

    	}catch (Exception ex) {
    		Assert.fail("aucune exception ne devrait être remontée"+ ex.toString());
    	}finally {
    		dataBaseTestConfig.closeConnection(con);

    	}
    	return resultat;

    }

    public boolean verifierDisponibiliteParking () {
    	Connection con = null;
    	boolean resultat = false;
    	final String VERIFIER_DISPONIBILITE_PARKING = "select AVAILABLE from parking where PARKING_NUMBER = 1;";
    	try{
    		PreparedStatement verifierDispoParking = dataBaseTestConfig.getConnection().prepareStatement(VERIFIER_DISPONIBILITE_PARKING);
			ResultSet resultatDispoParking = verifierDispoParking.executeQuery();
			if(resultatDispoParking.next()) {
				resultat = resultatDispoParking.getBoolean(1);
			}
			dataBaseTestConfig.closeResultSet(resultatDispoParking);
			dataBaseTestConfig.closePreparedStatement(verifierDispoParking);

    	}catch (Exception ex) {
    		Assert.fail("aucune exception ne devrait être remontée" + ex.toString());
    	}finally {
    		dataBaseTestConfig.closeConnection(con);

    	}
    	return resultat;
    }

    public Ticket recupererTicket () {
    	Connection con = null;
    	Ticket ticket = null;
    	final String RECUPERER_TICKET = "select PARKING_NUMBER, ID, PRICE, IN_TIME, OUT_TIME, VEHICLE_REG_NUMBER from TICKET where VEHICLE_REG_NUMBER = 'ABCDEF' ORDER BY ID DESC ;";
    	try{
    		PreparedStatement recupererTicket = dataBaseTestConfig.getConnection().prepareStatement(RECUPERER_TICKET);
			ResultSet resultatTicket = recupererTicket.executeQuery();
			if(resultatTicket.next()) {
				ticket = new Ticket();
				ticket.setParkingSpot(recupererParkingSpot(resultatTicket.getInt(1)));
				//ParkingSpot parkingSpot = new ParkingSpot(resultatTicket.getInt(1), ParkingType.valueOf(resultatTicket.getString(6)),false);
                //ticket.setParkingSpot(parkingSpot);
                ticket.setId(resultatTicket.getInt(2));
                ticket.setVehicleRegNumber(resultatTicket.getString(6));
                ticket.setPrice(resultatTicket.getDouble(3));
                ticket.setInTime(resultatTicket.getTimestamp(4));
                ticket.setOutTime(resultatTicket.getTimestamp(5));
			}

			dataBaseTestConfig.closeResultSet(resultatTicket);
			dataBaseTestConfig.closePreparedStatement(recupererTicket);

    	}catch (Exception ex) {
    		Assert.fail("aucune exception ne devrait être remontée" + ex.toString());
    	}finally {
    		dataBaseTestConfig.closeConnection(con);

    	}
    	return ticket;
    }
    public ParkingSpot recupererParkingSpot(int parking_number) {
    	Connection con = null;
    	ParkingSpot parkingSpot = null;
    	final String RECUPERER_PARKING_SPOT = "select PARKING_NUMBER, AVAILABLE, TYPE from PARKING where PARKING_NUMBER=?;";
    	try{
    		PreparedStatement recupererParkingSpot = dataBaseTestConfig.getConnection().prepareStatement(RECUPERER_PARKING_SPOT);
			recupererParkingSpot.setInt(1, parking_number);
    		ResultSet resultatParking = recupererParkingSpot.executeQuery();
			if(resultatParking.next()) {
				parkingSpot = new ParkingSpot(resultatParking.getInt(1), ParkingType.valueOf(resultatParking.getString(3)),resultatParking.getBoolean(2));
			}
			dataBaseTestConfig.closeResultSet(resultatParking);
			dataBaseTestConfig.closePreparedStatement(recupererParkingSpot);

    	}catch (Exception ex) {
    		Assert.fail("aucune exception ne devrait être remontée" + ex.toString());
    	}finally {
    		dataBaseTestConfig.closeConnection(con);

    	}
    	return parkingSpot;
    }

    public int nombreTicket (Ticket ticket) {
    	int nbTicket = 0;
    	Connection con = null;
    	try {
    		con = dataBaseTestConfig.getConnection();
    		PreparedStatement ps = con.prepareStatement(DBConstants.GET_NB_TICKET);
    		ps.setString(1, ticket.getVehicleRegNumber());
            ResultSet rs = ps.executeQuery();
            	if (rs.next()){
            		nbTicket = rs.getInt(1);

            	}
                dataBaseTestConfig.closeResultSet(rs);
                dataBaseTestConfig.closePreparedStatement(ps);
	    }catch (Exception ex){
	        Assert.fail("Error get number ticket same vehicle" + ex.toString());
	    }finally {
	        dataBaseTestConfig.closeConnection(con);
	    }
    	return nbTicket;

    }
}
