package com.example.trianaandaluciaprietogalvan.helloworldsupport.sync;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.example.trianaandaluciaprietogalvan.helloworldsupport.R;
import com.example.trianaandaluciaprietogalvan.helloworldsupport.data.MonitorECGContrato;
import com.example.trianaandaluciaprietogalvan.helloworldsupport.data.MonitorECGContrato.PruebaEntry;
import com.example.trianaandaluciaprietogalvan.helloworldsupport.data.MonitorECGContrato.ReporteEntry;
import com.example.trianaandaluciaprietogalvan.helloworldsupport.entities.Cardiologo;
import com.example.trianaandaluciaprietogalvan.helloworldsupport.entities.Paciente;
import com.example.trianaandaluciaprietogalvan.helloworldsupport.entities.Prueba;
import com.example.trianaandaluciaprietogalvan.helloworldsupport.entities.Reporte;
import com.example.trianaandaluciaprietogalvan.helloworldsupport.utils.CardiologoDAO;
import com.example.trianaandaluciaprietogalvan.helloworldsupport.utils.PacienteDAO;
import com.example.trianaandaluciaprietogalvan.helloworldsupport.web.ServicioWeb;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by trianaandaluciaprietogalvan on 05/04/16.
 */
public class MonitorECGSync extends AbstractThreadedSyncAdapter {

    public static final String SINCRONIZACION = "sincronizacion";
    public static final int SINCRONIZACION_PRUEBA = 100;
    public static final int SINCRONIZACION_ELECTROCARDIOGRAMA = 200;

    public static final String PARAM_EMAIL = "email";
    public static final String PARAM_PACIENTE = "paciente";

    //Proyeccion para verificar si el cardiologo ya existe
    public static final String[] PROYECCION_VERIFICAR_PRUEBA = new String[]{
            PruebaEntry.TABLE_NAME+"."+ PruebaEntry._ID
    };

    public static final String CAMPO_NOM_ARCHIVO  = "nom_prueba_ecg";



    //SELECCION PARA VERIFICAR REGISTROS DE PACIENTE A ACTUALZIAR
    public static final String SELECCION_UPDATE_PACIENTE = MonitorECGContrato.PacienteEntry.TABLE_NAME
                                                            +"."+ MonitorECGContrato.PacienteEntry.BANDERA_ACTUALIZAR+"=1";

    //SELECCION PARA REGRESAR LA BANDERA DE ACTUALIZAR  A 0
    public static final String SELECCION_UPDATE_BANDERA = MonitorECGContrato.PacienteEntry.TABLE_NAME
            +"."+ MonitorECGContrato.PacienteEntry._ID +"=?";
    //Columna del id del cardiologo
    public static final int COLUMN_ID_PRUEBA = 0;


    //Content resolver
    ContentResolver rs = getContext().getContentResolver();

    ContentResolver mContentResolver;

    ServicioWeb webService;

    public MonitorECGSync(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContentResolver = context.getContentResolver();
    }

    //mantener compatibilidad con versiones android 3.0 y posteriores
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public MonitorECGSync(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);

        mContentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        int val = extras.getInt(SINCRONIZACION);

        switch (val){
            case SINCRONIZACION_PRUEBA:
                String email = extras.getString(PARAM_EMAIL);
                try {
                    Response<List<Prueba>> pruebas = ServicioWeb.obtenerPruebas(email);
                    if(pruebas.isSuccessful()){
                        List<Prueba> lista = pruebas.body();
                        if(lista.size() != 0){
                            //verificar que las pruebas no esten registradas en la bd
                            List<Prueba> pruebasNoExistentes = verificarPruebasExistentes(lista);
                            //No hay pruebas por insertar
                            if(pruebasNoExistentes.size() != 0){
                                insertarPruebas(pruebasNoExistentes);
                            }
                        }
                    }
                    else{
                        Log.e("MonitorECGSync","Error al obtener la lista de las pruebas");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case SINCRONIZACION_ELECTROCARDIOGRAMA:
                enviarArchivo("");
                break;
            default:
        }

        //verificar si hay registros por actualizar
        Cursor cursor = rs.query(MonitorECGContrato.PacienteEntry.CONTENT_URI, PacienteDAO.COLUMNS_PACIENTE, SELECCION_UPDATE_PACIENTE, null, null);
        //hay registros de pacientes pendientes para actualizar
        if(cursor.getCount() != 0){
            Paciente p = new Paciente();
            cursor.moveToFirst();
            p.idPaciente = cursor.getInt(PacienteDAO.COLUMN_ID_P);
            p.nombre = cursor.getString(PacienteDAO.COLUMN_NOMBRE_P);
            p.apellidoPaterno = cursor.getString(PacienteDAO.COLUMN_APP_P);
            p.apellidoMaterno = cursor.getString(PacienteDAO.COLUMN_APM_P);
            p.curp = cursor.getString(PacienteDAO.COLUMN_CURP_P);
            p.edad = cursor.getInt(PacienteDAO.COLUMN_EDAD_P);
            String sexo = cursor.getString(PacienteDAO.COLUMN_SEXO_P);
            p.sexo = sexo.charAt(0);
            p.correo = cursor.getString(PacienteDAO.COLUMN_CORREO_P);
            p.telefono = cursor.getString(PacienteDAO.COLUMN_TELEFONO_P);
            p.contrasena = cursor.getString(PacienteDAO.COLUMN_CONTRASENA_P);
            p.frecuenciaRespiratoria = cursor.getInt(PacienteDAO.COLUMN_FRECUENCIA_P);
            p.presionSistolica = cursor.getInt(PacienteDAO.COLUMN_PRESION_SIS_P);
            p.presionDiastolica = cursor.getInt(PacienteDAO.COLUMN_PRESION_DIAS_P);
            p.imc = cursor.getInt(PacienteDAO.COLUMN_IMC_P);
            p.altura = cursor.getDouble(PacienteDAO.COLUMN_ALTURA_P);
            p.peso = cursor.getInt(PacienteDAO.COLUMN_PESO_P);
            p.cardiologo = new Cardiologo();
            p.fechamodificacion = cursor.getString(PacienteDAO.COLUMN_FECHA_MODIFICACION);
            p.cardiologo.idCardiologo = cursor.getInt(PacienteDAO.COLUMN_CARDIOLOGO_ID_CARDIOLOGO);

            String actualizarBandera;
            try {
                Response<String> resp = ServicioWeb.actualizarPaciente(p);
                if(resp.isSuccessful()){
                    actualizarBandera = resp.body();
                    if(actualizarBandera.equals("ok")){
                        ContentValues cv = new ContentValues();
                        cv.put(MonitorECGContrato.PacienteEntry.BANDERA_ACTUALIZAR,0);

                        String[] args = new String[]{
                                Integer.toString(p.idPaciente)
                        };

                        //regresar la bandera de actualizar a 0
                        rs.update(MonitorECGContrato.PacienteEntry.CONTENT_URI, cv, SELECCION_UPDATE_BANDERA,args);
                    }else{
                        Log.e("MonitorECGSync","Error al actualizar los datos con el servidor");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public MultipartBody.Part buildMulripartFromFile(String fieldName, String filePath) {
        MultipartBody.Part archivo = null;
        if(filePath != null) {
            File file = new File(filePath);
            RequestBody filePart = RequestBody.create(MediaType.parse("multipart/form-data"), file);

            archivo = MultipartBody.Part.createFormData("filePrueba",file.getName(), filePart);
        }
        return archivo;
    }


    private  void enviarArchivo(String filePath) {
        MultipartBody.Part prueba = buildMulripartFromFile(CAMPO_NOM_ARCHIVO, filePath);
        try {
            Response<Prueba> p  = ServicioWeb.crearPrueba(prueba);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void insertarPruebas(List<Prueba> pruebas){

        for (Prueba p : pruebas){
            ContentValues contentValues = new ContentValues();
            contentValues.put(PruebaEntry._ID,p.idPrueba);
            contentValues.put(PruebaEntry.COLUMN_FECHA,p.fecha);
            contentValues.put(PruebaEntry.COLUMN_FECHA_ENVIO,p.fechaEnvio);
            contentValues.put(PruebaEntry.COLUMN_HORA,p.hora);
            contentValues.put(PruebaEntry.COLUMN_HORA_ENVIO,p.horaEnvio);
            contentValues.put(PruebaEntry.COLUMN_OBSERVACIONES,p.observaciones);
            contentValues.put(PruebaEntry.COLUMN_PACIENTE_ID_PACIENTE,p.paciente.idPaciente);
            contentValues.put(PruebaEntry.COLUMN_FRECUENCIA_CARDIACA,p.frecuenciaCardiaca);

            //crear el reporte
            insertarReporte(p.reporte);

            contentValues.put(PruebaEntry.COLUMN_REPORTE_ID_REPORTE, p.reporte.idReporte);

            mContentResolver.insert(PruebaEntry.CONTENT_URI,contentValues);
        }
    }

    public List<Prueba> verificarPruebasExistentes(List<Prueba> pruebas){
        List<Prueba> listaNuevasPruebas = new ArrayList<>();
        for (Prueba prueba : pruebas){
            Uri uriPruebaId = PruebaEntry.buildPruebaId(prueba.idPrueba);
            Cursor cursor = rs.query(uriPruebaId, PROYECCION_VERIFICAR_PRUEBA, null, null, null);
            if(cursor != null){
                //verificar si el cursor tiene datos
                int count = cursor.getCount();
                cursor.close();
                //no existe el cardiologo regstrado en la bd
                if(count == 0)
                    listaNuevasPruebas.add(prueba);
            }
        }
        return listaNuevasPruebas;
    }

    public void insertarReporte(Reporte reporte){
        //verificar si existe el cardiologo registrado en la llave foranea del reporte
        //verificar si el cardiologo existe en la bd
        Cardiologo c = new Cardiologo();
        c.idCardiologo = reporte.idCardiologo;
        verificarRegistroMedico(c);

        ContentValues contentValues = new ContentValues();
        contentValues.put(ReporteEntry.COLUMN_ESTATUS,reporte.estatus);
        contentValues.put(ReporteEntry._ID,reporte.idReporte);
        contentValues.put(ReporteEntry.COLUMN_RECOMENDACIONES,reporte.recomendaciones);
        contentValues.put(ReporteEntry.COLUMN_CARDIOLOGO_ID_CARDIOLOGO,reporte.idCardiologo);

        mContentResolver.insert(ReporteEntry.CONTENT_URI,contentValues);
    }

    public static void syncInmediatly(Context context,Account account,Bundle bundle){
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(account, context.getString(R.string.content_authority), bundle);
    }

    public void verificarRegistroMedico(final Cardiologo car){
        Uri uriCardiologoId = MonitorECGContrato.CardiologoEntry.buildCardiologoId(car.idCardiologo);
        Cursor cursor = rs.query(uriCardiologoId, CardiologoDAO.PROYECCION_VERIFICAR_CARDIOLOGO, null, null, null);
        //verificar si el cursor tiene datos
        int count = cursor.getCount();
        //no existe el cardiologo regstrado en la bd
        if(count == 0){
            //obtener el cardiologo del servidor
            ServicioWeb.obtenerCardiologo(car, new Callback<Cardiologo>() {
                @Override
                public void onResponse(Call<Cardiologo> call, Response<Cardiologo> response) {
                    Cardiologo cardiologo = response.body();
                    CardiologoDAO.insertarCardiologo(cardiologo, rs);
                }

                @Override
                public void onFailure(Call<Cardiologo> call, Throwable t) {
                    Log.e("LoginFinal", "No se obtuvó el cardiologo con el id: " + car.idCardiologo);
                }
            });
        }
    }
}
