package com.example.app.uber.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.location.LocationListenerCompat;

import com.example.app.uber.R;
import com.example.app.uber.databinding.ActivityPassageiroBinding;
import com.example.app.uber.firebase.FireBase;
import com.example.app.uber.helper.Local;
import com.example.app.uber.helper.UsuarioFirebase;
import com.example.app.uber.model.Destino;
import com.example.app.uber.model.Requisicao;
import com.example.app.uber.model.Usuario;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PassageiroActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ActivityPassageiroBinding binding;
    private GoogleMap mMap;
    private FirebaseAuth auth;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Button chamarUber;
    private LinearLayout linearLayoutDestino;
    private EditText textDestino;
    private LatLng localPassageiro;
    private boolean cancelarUber = false;
    private DatabaseReference firebaseRef;
    private Requisicao requisicao;
    private Usuario passageiro;
    private Usuario motorista;
    private String status;
    private Destino destino;
    private Marker marcadorMotorista;
    private Marker marcadorPassageiro;
    private Marker marcadorDestino;
    private LatLng localMotorista;
    private ValueEventListener valueEventListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPassageiroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        //Configuraçao inicial

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        chamarUber = binding.chamarUber;
        textDestino = binding.editDestino;
        linearLayoutDestino = binding.linearLayoutDestino;
        firebaseRef = FireBase.getFirebaseDatabase();

        binding.toolbar.setTitle("Iniciar uma viagem");
        setSupportActionBar(binding.toolbar);

        buttonChamarUber();

        //Adiciona listener para status da requisição
        verificaStatusRequisicao();

    }

    private void verificaStatusRequisicao() {

        Usuario usuario = UsuarioFirebase.getDadosUsuarioLogado();
        String idUsuario = usuario.getId();

        DatabaseReference requisicoes = firebaseRef.child("requisicoes");

        Query requisicaoPesquisa = requisicoes.orderByChild("passageiro/id")
                .equalTo(idUsuario);

        valueEventListener = requisicaoPesquisa.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                List<Requisicao> requisicaoList = new ArrayList<>();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    requisicaoList.add(ds.getValue(Requisicao.class));
                }

                if (requisicaoList.size() != 0) {
                    Collections.reverse(requisicaoList);
                    requisicao = requisicaoList.get(0);

                    if (requisicao != null) {
                        if (!requisicao.getStatus().equals(Requisicao.STATUS_ENCERRADA)) {

                            passageiro = requisicao.getPassageiro();
                            localPassageiro = new LatLng(Double.parseDouble(passageiro.getLatitude()),
                                    Double.parseDouble(passageiro.getLongitude()));

                            status = requisicao.getStatus();
                            destino = requisicao.getDestino();
                            if (requisicao.getMotorista() != null) {
                                motorista = requisicao.getMotorista();
                                localMotorista = new LatLng(
                                        Double.parseDouble(motorista.getLatitude()),
                                        Double.parseDouble(motorista.getLongitude())
                                );
                            }
                            alterarInterfaceStatusRequisicao(status);
                        }
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void alterarInterfaceStatusRequisicao(String status) {

        if (status != null && !status.isEmpty()) {
            cancelarUber = false;
            switch (status) {
                case Requisicao.STATUS_AGUARDANDO:
                    requisicaoAguardando();
                    break;
                case Requisicao.STATUS_A_CAMINHO:
                    requisicaoAcaminho();
                    break;
                case Requisicao.STATUS_VIAGEM:
                    requisicaoViagem();
                    break;
                case Requisicao.STATUS_FINALIZADA:
                    requisicaoFinalizada();
                    break;
                case Requisicao.STATUS_CANCELADA:
                    requisicaoCancelada();
                    break;
            }
        }else {
            //Adicionar marcador de passageiro
            adicionarMarcadorPassageiro(localPassageiro,"Seu local");
            centralizarMarcador(localPassageiro);
        }
    }

    @SuppressLint("SetTextI18n")
    private void requisicaoCancelada(){
        linearLayoutDestino.setVisibility(View.VISIBLE);
        chamarUber.setText("Chamar Uber");
        cancelarUber = false;
    }

    @SuppressLint("SetTextI18n")
    private void requisicaoAguardando() {
        linearLayoutDestino.setVisibility(View.GONE);
        chamarUber.setText("Cancelar Uber");
        cancelarUber = true;

        //Adicionar marcador passageiro
        adicionarMarcadorPassageiro(localPassageiro, passageiro.getNome());
        centralizarMarcador(localPassageiro);
    }

    @SuppressLint("SetTextI18n")
    private void requisicaoAcaminho() {
        chamarUber.setEnabled(false);
        linearLayoutDestino.setVisibility(View.GONE);
        chamarUber.setText("Motorista a caminho");

        //Adicionar marcador de passageiro
        adicionarMarcadorPassageiro(localPassageiro, passageiro.getNome());

        //Adicionar marcador de motorista
        adicionarMarcadorMotorista(localMotorista, motorista.getNome());


        //Centralizar passageiro/motorista
        centralizarDoisMarcadores(marcadorMotorista, marcadorPassageiro);
    }


    @SuppressLint("SetTextI18n")
    private void requisicaoViagem() {
        chamarUber.setEnabled(false);
        linearLayoutDestino.setVisibility(View.GONE);
        chamarUber.setText("A caminho do destino");

        //Adicionar marcador de motorista
        adicionarMarcadorMotorista(localMotorista, motorista.getNome());
        //Adicionar marcador de destino
        LatLng localDestino = new LatLng(
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude()));
        adicionarMarcadorDestino(localDestino, "Destino");

        //Centralizar marcador motorista/Destino
        centralizarDoisMarcadores(marcadorMotorista, marcadorDestino);

    }


    @SuppressLint("SetTextI18n")
    private void requisicaoFinalizada() {
        chamarUber.setEnabled(false);
        linearLayoutDestino.setVisibility(View.GONE);
        chamarUber.setText("Corrida Finalizada");

        //Adicionar marcador de destino
        LatLng localDestino = new LatLng(
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude()));
        adicionarMarcadorDestino(localDestino, "Destino");
        centralizarMarcador(localDestino);

        //calcular distancia
        float distancia = Local.calcularDistancia(localPassageiro, localDestino);
        float valor = distancia * 8;
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        String resultado = decimalFormat.format(valor);
        chamarUber.setText("Corrida finalizada - R$ " + resultado);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Total da viagem")
                .setMessage("Sua viagem ficou: R$ " + resultado)
                .setCancelable(false)
                .setNegativeButton("Encerrar Viagem", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        requisicao.setStatus(Requisicao.STATUS_ENCERRADA);
                        requisicao.atualizarStatus();
                        finish();
                        startActivity(new Intent(getIntent()));
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void adicionarMarcadorDestino(LatLng localizacaoMot, String titulo) {

        if (marcadorPassageiro != null) {
            marcadorPassageiro.remove();
        }
        if (marcadorDestino != null) {
            marcadorDestino.remove();
        }
        marcadorDestino = mMap.addMarker(new MarkerOptions()
                .position(localizacaoMot)
                .title(titulo)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.destino)));
    }

    private void centralizarDoisMarcadores(Marker marcador1, Marker marcador2) {

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(marcador1.getPosition());
        builder.include(marcador2.getPosition());

        LatLngBounds bounds = builder.build();

        int largura = getResources().getDisplayMetrics().widthPixels;
        int altura = getResources().getDisplayMetrics().heightPixels;
        int espacoInterno = (int) (largura * 0.40);

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, largura, altura, espacoInterno));
    }

    private void adicionarMarcadorMotorista(LatLng localizacaoMot, String titulo) {

        if (marcadorMotorista != null) {
            marcadorMotorista.remove();
        }

        marcadorMotorista = mMap.addMarker(new MarkerOptions()
                .position(localizacaoMot)
                .title(titulo)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.carro)));
    }

    private void adicionarMarcadorPassageiro(LatLng localizacaoPas, String titulo) {

        if (marcadorPassageiro != null) {
            marcadorPassageiro.remove();
        }
        marcadorPassageiro = mMap.addMarker(new MarkerOptions()
                .position(localizacaoPas)
                .title(titulo)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario)));
    }

    private void centralizarMarcador(LatLng local) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(local, 20));
    }


    private void buttonChamarUber() {

        //Chamar Uber button
        chamarUber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //false -> uber não pode ser cancelado ainda
                //true -> uber pode ser cancelado

                if (cancelarUber) { //Uber pode ser cancelado
                    //Cancelar a requisição
                    requisicao.setStatus(Requisicao.STATUS_CANCELADA);
                    requisicao.atualizarStatus();
                } else {

                    //Inicio
                    String enderecodestino = textDestino.getText().toString();

                    if (!enderecodestino.equals("")) {

                        Address addressDestino = null;
                        try {
                            addressDestino = recuperarEndereco(enderecodestino);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (addressDestino != null) {

                            Destino destino = new Destino();

                            destino.setCidade(addressDestino.getSubAdminArea());
                            destino.setCep(addressDestino.getPostalCode());
                            destino.setBairro(addressDestino.getSubLocality());
                            destino.setRua(addressDestino.getThoroughfare());
                            destino.setLatitude(String.valueOf(addressDestino.getLatitude()));
                            destino.setLongitude(String.valueOf(addressDestino.getLongitude()));

                            StringBuilder mensagem = new StringBuilder();

                            mensagem.append("Cidade: " + destino.getCidade())
                                    .append("\nRua: " + destino.getRua())
                                    .append("\nBairro: " + destino.getBairro())
                                    .append("\nCep: " + destino.getCep());

                            AlertDialog.Builder builder = new AlertDialog
                                    .Builder(PassageiroActivity.this);

                            builder.setTitle("Endereço");
                            builder.setMessage(mensagem);
                            builder.setPositiveButton("Confimar",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                            //Salvar requisição
                                            salvarResquisicao(destino);
                                        }
                                    });
                            builder.setNegativeButton("Cancelar",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    });

                            AlertDialog dialog = builder.create();
                            dialog.show();

                        } else {
                            Toast.makeText(PassageiroActivity.this,
                                    "Não foi possível recuperar os dados",
                                    Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(PassageiroActivity.this,
                                "informe o endereço de destino!",
                                Toast.LENGTH_SHORT).show();
                    }
                    //Fim
                }
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void salvarResquisicao(Destino destino) {
        Requisicao requisicao = new Requisicao();

        requisicao.setDestino(destino);
        Usuario usuarioPassageiro = UsuarioFirebase.getDadosUsuarioLogado();
        usuarioPassageiro.setLatitude(String.valueOf(localPassageiro.latitude));
        usuarioPassageiro.setLongitude(String.valueOf(localPassageiro.longitude));
        requisicao.setPassageiro(usuarioPassageiro);
        requisicao.setStatus(Requisicao.STATUS_AGUARDANDO);
        requisicao.salvar();

        linearLayoutDestino.setVisibility(View.GONE);
        chamarUber.setText("Cancelar Uber");
    }

    public Address recuperarEndereco(String endereco) throws IOException {


        Geocoder geocoder = new Geocoder(PassageiroActivity.this);
        List<Address> addressList = null;

        try {
            addressList = geocoder.getFromLocationName(endereco,
                    1);

            if (addressList.size() > 0) {
                return addressList.get(0);
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();

        } catch (NetworkOnMainThreadException e) {

            while (addressList == null) {
                addressList = geocoder.getFromLocationName(endereco,
                        1);
            }
            if (addressList.size() > 0) {
                return addressList.get(0);
            } else {
                return null;
            }
        }
        return null;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        //Recuperar localização usuário
        recuperarLocalizacaoUsuario();
    }

    private void recuperarLocalizacaoUsuario() {

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListenerCompat() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                //recuperar latitude e longitude
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                localPassageiro = new LatLng(latitude, longitude);

                //Atualizar no GeoFire
                UsuarioFirebase.atualizarDadosLocalizacao(latitude, longitude);

                //Alterar interface de acordo com o status
                alterarInterfaceStatusRequisicao(status);
                if (status != null && !status.isEmpty()) {
                    if (status.equals(Requisicao.STATUS_VIAGEM) ||
                            status.equals(Requisicao.STATUS_FINALIZADA)) {
                        locationManager.removeUpdates(locationListener);
                    }else {
                        //Solicitar atualização de localização
                        if (ActivityCompat.checkSelfPermission(PassageiroActivity.this,
                                Manifest.permission.ACCESS_FINE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED) {
                            locationManager.requestLocationUpdates(
                                    LocationManager.GPS_PROVIDER,
                                    10000,
                                    10,
                                    locationListener);
                        }

                    }
                }
            }
        };

        //Solicitar atualização de localização
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10000,
                    10,
                    locationListener);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuSair) {
            auth = FireBase.getFirebaseAuth();
            auth.signOut();
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onStop() {
        if (firebaseRef != null) {
            firebaseRef.removeEventListener(valueEventListener);
        }
        super.onStop();
    }
}