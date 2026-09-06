package com.example.primeiroprojeto;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSAO_MICROFONE = 1;

    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private AudioManager audioManager;

    private Thread threadAudio;
    private boolean gravando = false;

    private TextView txtNivel;
    private ProgressBar barraNivel;
    private WaveformView formaOnda;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        Button botao = findViewById(R.id.btnMicrofone);
        txtNivel = findViewById(R.id.txtNivel);
        barraNivel = findViewById(R.id.barraNivel);
        formaOnda = findViewById(R.id.formaOnda);

        botao.setOnClickListener(v -> {

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        PERMISSAO_MICROFONE);

            } else {
                iniciarMicrofone();
            }
        });
    }

    private void iniciarMicrofone() {

        int taxaAmostragem = 12000;

        int bufferMinimo = AudioRecord.getMinBufferSize(
                taxaAmostragem,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (bufferMinimo <= 0) {
            Toast.makeText(this,
                    "Não foi possível obter o buffer do microfone",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Procura um dispositivo Bluetooth que possa
        // ser usado para comunicação de voz.
        AudioDeviceInfo dispositivoBluetooth = null;

        AudioDeviceInfo[] dispositivos =
                audioManager.getAvailableCommunicationDevices()
                        .toArray(new AudioDeviceInfo[0]);

        for (AudioDeviceInfo dispositivo : dispositivos) {

            if (dispositivo.getType()
                    == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {

                dispositivoBluetooth = dispositivo;
                break;
            }
        }

        if (dispositivoBluetooth == null) {

            Toast.makeText(this,
                    "Fone Bluetooth não disponível para comunicação de voz",
                    Toast.LENGTH_LONG).show();

            return;
        }

        // Seleciona o Bluetooth como dispositivo
        // de comunicação.
        boolean selecionado =
                audioManager.setCommunicationDevice(
                        dispositivoBluetooth);

        if (!selecionado) {

            Toast.makeText(this,
                    "Não foi possível selecionar o fone Bluetooth",
                    Toast.LENGTH_LONG).show();

            return;
        }

        Toast.makeText(this,
                "Fone Bluetooth selecionado para comunicação",
                Toast.LENGTH_SHORT).show();

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                taxaAmostragem,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferMinimo * 2);

        AudioFormat formatoSaida =
                new AudioFormat.Builder()
                        .setSampleRate(taxaAmostragem)
                        .setEncoding(
                                AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(
                                AudioFormat.CHANNEL_OUT_MONO)
                        .build();

        audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(
                                        AudioAttributes.USAGE_VOICE_COMMUNICATION)
                                .setContentType(
                                        AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build())
                .setAudioFormat(formatoSaida)
                .setBufferSizeInBytes(bufferMinimo * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();

        audioRecord.startRecording();
        audioTrack.play();

        gravando = true;

        threadAudio = new Thread(() -> {

            short[] buffer = new short[bufferMinimo];

            while (gravando) {

                int quantidade = audioRecord.read(
                        buffer, 0, buffer.length);

                if (quantidade > 0) {

                    audioTrack.write(
                            buffer, 0, quantidade);

                    double somaQuadrados = 0;

                    for (int i = 0; i < quantidade; i++) {
                        somaQuadrados +=
                                (double) buffer[i] * buffer[i];
                    }

                    double rms =
                            Math.sqrt(
                                    somaQuadrados / quantidade);

                    final double nivelDb;

                    if (rms > 0) {
                        nivelDb = 20 *
                                Math.log10(
                                        rms / 32767.0);
                    } else {
                        nivelDb = -100;
                    }

                    short[] amostras = buffer.clone();

                    runOnUiThread(() -> {

                        txtNivel.setText(
                                String.format(
                                        "Nível: %.1f dB",
                                        nivelDb));

                        int valorBarra =
                                (int) ((nivelDb + 60)
                                        * 32767 / 60);

                        if (valorBarra < 0)
                            valorBarra = 0;

                        if (valorBarra > 32767)
                            valorBarra = 32767;

                        barraNivel.setProgress(
                                valorBarra);

                        formaOnda.setSamples(
                                amostras);
                    });
                }
            }
        });

        threadAudio.start();
    }

    @Override
    protected void onDestroy() {

        gravando = false;

        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }

        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }

        if (audioManager != null) {
            audioManager.clearCommunicationDevice();
        }

        super.onDestroy();
    }
}