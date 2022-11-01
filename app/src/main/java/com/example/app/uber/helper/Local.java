package com.example.app.uber.helper;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;

public class Local {
    public static float calcularDistancia(LatLng latLngInicial, LatLng latLngFinal){
        Location loccalInicial = new Location("Local inicial");
        loccalInicial.setLatitude(latLngInicial.latitude);
        loccalInicial.setLongitude(latLngInicial.longitude);

        Location loccalFinal = new Location("Local final");
        loccalFinal.setLatitude(latLngFinal.latitude);
        loccalFinal.setLongitude(latLngFinal.longitude);

        //Caulcular distancia - Resultado em metros
        // dividir por 1000 para converter em KM
         float distancia = loccalInicial.distanceTo(loccalFinal) / 1000;
        return distancia;
    }
    public static String formatarDistancia(float distancia){
        String distanciaFormatada;
        if(distancia<1){
            distancia = distancia * 1000; //Em Metros
            distanciaFormatada = Math.round(distancia) + " M ";
        }else {
            DecimalFormat decimal = new DecimalFormat("0.0");
            distanciaFormatada = decimal.format(distancia) + " KM ";
        }
        return distanciaFormatada;
    }
}
