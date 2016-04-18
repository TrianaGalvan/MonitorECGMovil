package com.example.trianaandaluciaprietogalvan.helloworldsupport.web;

import com.example.trianaandaluciaprietogalvan.helloworldsupport.entities.Cardiologo;
import com.example.trianaandaluciaprietogalvan.helloworldsupport.entities.Paciente;
import com.example.trianaandaluciaprietogalvan.helloworldsupport.entities.Prueba;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by trianaandaluciaprietogalvan on 04/04/16.
 */
public class ServicioWeb {
    public static Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("http://192.168.0.5:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();


    public static void loginPaciente(String correo, String pass, Callback<Paciente> respuesta){
        PacienteService pacienteService = retrofit.create(PacienteService.class);
        Call<Paciente> call = pacienteService.loginPaciente(correo,pass);
        call.enqueue(respuesta);
    }

    public static void verfificarCorreo(String correo,Callback<Boolean> respuesta){
        PacienteService pacienteService = retrofit.create(PacienteService.class);
        Call<Boolean> call = pacienteService.verificarCorreo(correo);
        call.enqueue(respuesta);
    }

    public static void obtenerCardiologos(Callback<List<Cardiologo>> respuesta){
        CardiologoService cardiologoService = retrofit.create(CardiologoService.class);
        Call<List<Cardiologo>> call = cardiologoService.obtenerCardiologos();
        call.enqueue(respuesta);
    }

    public static void insertarPaciente(Paciente paciente,Callback<Paciente> respuesta) {
        PacienteService pacienteService = retrofit.create(PacienteService.class);
        Call<Paciente> call = pacienteService.insertarPaciente(paciente);
        call.enqueue(respuesta);
    }

    public static Response<List<Prueba>> obtenerPruebas(String correo) throws IOException {
        PruebaService pruebaService = retrofit.create(PruebaService.class);
        Call<List<Prueba>> call = pruebaService.obtenerPruebas(correo);
        return call.execute();
    }

    public static void obtenerCardiologo(Cardiologo car,Callback<Cardiologo> callback) {
        CardiologoService cardiologoService = retrofit.create(CardiologoService.class);
        Call<Cardiologo> call = cardiologoService.obtenerCardiologo(car.idCardiologo);
        call.enqueue(callback);
    }

    public static Response<String> actualizarPaciente(Paciente paciente) throws IOException {
        PacienteService pacienteService = retrofit.create(PacienteService.class);
        Call<String> call = pacienteService.actualizarPaciente(paciente.idPaciente, paciente);
        return call.execute();
    }
}
