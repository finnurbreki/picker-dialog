// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import org.chromium.base.BuildInfo;
import org.chromium.base.Log;
import org.chromium.base.ThreadUtils;
import org.chromium.base.task.AsyncTask;
import org.chromium.base.task.TaskRunner;
import org.chromium.base.task.TaskTraits;
import org.chromium.net.MimeTypeFilter;
//import org.chromium.ui.base.WindowAndroid;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A worker task to enumerate image files on disk.
 */
class FileEnumWorkerTask extends AsyncTask<List<PickerBitmap>> {
    // A tag for logging error messages.
    private static final String TAG = "PhotoPicker";

    /**
     * An interface to use to communicate back the results to the client.
     */
    public interface FilesEnumeratedCallback {
        /**
         * A callback to define to receive the list of all images on disk.
         * @param files The list of images, or null if the function fails.
         */
        void filesEnumeratedCallback(List<PickerBitmap> files);
    }

    //private final WindowAndroid mWindowAndroid;

    // The callback to use to communicate the results.
    private FilesEnumeratedCallback mCallback;

    // The filter to apply to the list.
    private MimeTypeFilter mFilter;

    // Whether any image MIME types were requested.
    private boolean mIncludeImages;

    // Whether any video MIME types were requested.
    private boolean mIncludeVideos;

    // The ContentResolver to use to retrieve image metadata from disk.
    private ContentResolver mContentResolver;

    // The camera directory under DCIM.
    private static final String SAMPLE_DCIM_SOURCE_SUB_DIRECTORY = "Camera";

    // ----------------------------------------------------------------
    // Debug decoding with index instead of file path:
    // ----------------------------------------------------------------
    private static int mOrdinal = 0;

    // Works for the case when you don't need the decoder to be in the same process as the app (for
    // debugging). Once it is in the same process, all requests are serialized.
    public static final Map<String, Integer> mFileIndex = new HashMap<>();
    // mFileIndex.put(uri.getPath(), ++mOrdinal); <- Add just before adding to pickerBitmaps.

    // If you need to show the index in DecoderService, it is better to switch to a hard-coded
    // map (remember to call this from the ctor):
    //     private void populateMap() {
    //         mFileIndex.put("/external/file/124482", 1);
    //         mFileIndex.put("/external/file/124479", 2);
    //         ....
    //     }
    //
    // Construct this using:
    //     Log.e("chromfb", "mFileIndex.put(\"" + uri.getPath() + "\", " + (++mOrdinal) + ");");
    //
    // ----------------------------------------------------------------

    /**
     * A FileEnumWorkerTask constructor.
     * @param windowAndroid The window wrapper associated with the current activity.
     * @param callback The callback to use to communicate back the results.
     * @param filter The file filter to apply to the list.
     * @param contentResolver The ContentResolver to use to retrieve image metadata from disk.
     */
    public FileEnumWorkerTask(/*WindowAndroid windowAndroid,*/ FilesEnumeratedCallback callback,
            MimeTypeFilter filter, List<String> mimeTypes, ContentResolver contentResolver) {
        populateMap();

        //mWindowAndroid = windowAndroid;
        mCallback = callback;
        mFilter = filter;
        mContentResolver = contentResolver;

        for (String mimeType : mimeTypes) {
            if (mimeType.startsWith("image/")) {
                mIncludeImages = true;
            } else if (mimeType.startsWith("video/")) {
                mIncludeVideos = true;
            }

            if (mIncludeImages && mIncludeVideos) break;
        }
    }

    public static void populateMap() {
        mFileIndex.put("/external/file/124482", 1);
        mFileIndex.put("/external/file/124479", 2);
        mFileIndex.put("/external/file/124478", 3);
        mFileIndex.put("/external/file/124476", 4);
        mFileIndex.put("/external/file/124472", 5);
        mFileIndex.put("/external/file/124470", 6);
        mFileIndex.put("/external/file/124468", 7);
        mFileIndex.put("/external/file/124469", 8);
        mFileIndex.put("/external/file/124463", 9);
        mFileIndex.put("/external/file/124464", 10);
        mFileIndex.put("/external/file/124460", 11);
        mFileIndex.put("/external/file/124459", 12);
        mFileIndex.put("/external/file/124455", 13);
        mFileIndex.put("/external/file/124452", 14);
        mFileIndex.put("/external/file/124446", 15);
        mFileIndex.put("/external/file/124442", 16);
        mFileIndex.put("/external/file/124441", 17);
        mFileIndex.put("/external/file/124437", 18);
        mFileIndex.put("/external/file/124435", 19);
        mFileIndex.put("/external/file/124434", 20);
        mFileIndex.put("/external/file/124428", 21);
        mFileIndex.put("/external/file/124423", 22);
        mFileIndex.put("/external/file/124415", 23);
        mFileIndex.put("/external/file/124410", 24);
        mFileIndex.put("/external/file/124408", 25);
        mFileIndex.put("/external/file/124406", 26);
        mFileIndex.put("/external/file/124397", 27);
        mFileIndex.put("/external/file/124388", 28);
        mFileIndex.put("/external/file/124384", 29);
        mFileIndex.put("/external/file/124383", 30);
        mFileIndex.put("/external/file/124362", 31);
        mFileIndex.put("/external/file/124355", 32);
        mFileIndex.put("/external/file/124323", 33);
        mFileIndex.put("/external/file/124322", 34);
        mFileIndex.put("/external/file/124320", 35);
        mFileIndex.put("/external/file/124316", 36);
        mFileIndex.put("/external/file/124314", 37);
        mFileIndex.put("/external/file/124311", 38);
        mFileIndex.put("/external/file/124303", 39);
        mFileIndex.put("/external/file/124302", 40);
        mFileIndex.put("/external/file/124301", 41);
        mFileIndex.put("/external/file/124300", 42);
        mFileIndex.put("/external/file/124299", 43);
        mFileIndex.put("/external/file/124296", 44);
        mFileIndex.put("/external/file/124288", 45);
        mFileIndex.put("/external/file/124285", 46);
        mFileIndex.put("/external/file/124284", 47);
        mFileIndex.put("/external/file/124283", 48);
        mFileIndex.put("/external/file/124282", 49);
        mFileIndex.put("/external/file/124277", 50);
        mFileIndex.put("/external/file/124274", 51);
        mFileIndex.put("/external/file/124270", 52);
        mFileIndex.put("/external/file/124266", 53);
        mFileIndex.put("/external/file/124265", 54);
        mFileIndex.put("/external/file/124264", 55);
        mFileIndex.put("/external/file/124263", 56);
        mFileIndex.put("/external/file/124246", 57);
        mFileIndex.put("/external/file/124245", 58);
        mFileIndex.put("/external/file/124207", 59);
        mFileIndex.put("/external/file/124208", 60);
        mFileIndex.put("/external/file/124209", 61);
        mFileIndex.put("/external/file/124202", 62);
        mFileIndex.put("/external/file/124199", 63);
        mFileIndex.put("/external/file/124166", 64);
        mFileIndex.put("/external/file/124146", 65);
        mFileIndex.put("/external/file/124125", 66);
        mFileIndex.put("/external/file/124119", 67);
        mFileIndex.put("/external/file/124102", 68);
        mFileIndex.put("/external/file/124100", 69);
        mFileIndex.put("/external/file/124101", 70);
        mFileIndex.put("/external/file/124099", 71);
        mFileIndex.put("/external/file/124098", 72);
        mFileIndex.put("/external/file/124096", 73);
        mFileIndex.put("/external/file/124097", 74);
        mFileIndex.put("/external/file/124062", 75);
        mFileIndex.put("/external/file/124057", 76);
        mFileIndex.put("/external/file/124053", 77);
        mFileIndex.put("/external/file/123970", 78);
        mFileIndex.put("/external/file/123969", 79);
        mFileIndex.put("/external/file/123968", 80);
        mFileIndex.put("/external/file/123967", 81);
        mFileIndex.put("/external/file/123966", 82);
        mFileIndex.put("/external/file/123946", 83);
        mFileIndex.put("/external/file/122619", 84);
        mFileIndex.put("/external/file/122620", 85);
        mFileIndex.put("/external/file/122618", 86);
        mFileIndex.put("/external/file/122617", 87);
        mFileIndex.put("/external/file/122616", 88);
        mFileIndex.put("/external/file/122615", 89);
        mFileIndex.put("/external/file/122614", 90);
        mFileIndex.put("/external/file/122613", 91);
        mFileIndex.put("/external/file/122612", 92);
        mFileIndex.put("/external/file/122611", 93);
        mFileIndex.put("/external/file/122610", 94);
        mFileIndex.put("/external/file/122609", 95);
        mFileIndex.put("/external/file/122608", 96);
        mFileIndex.put("/external/file/122607", 97);
        mFileIndex.put("/external/file/122602", 98);
        mFileIndex.put("/external/file/122603", 99);
        mFileIndex.put("/external/file/122601", 100);
        mFileIndex.put("/external/file/122600", 101);
        mFileIndex.put("/external/file/122599", 102);
        mFileIndex.put("/external/file/122598", 103);
        mFileIndex.put("/external/file/122597", 104);
        mFileIndex.put("/external/file/122596", 105);
        mFileIndex.put("/external/file/122595", 106);
        mFileIndex.put("/external/file/122594", 107);
        mFileIndex.put("/external/file/122593", 108);
        mFileIndex.put("/external/file/122592", 109);
        mFileIndex.put("/external/file/122591", 110);
        mFileIndex.put("/external/file/122590", 111);
        mFileIndex.put("/external/file/122589", 112);
        mFileIndex.put("/external/file/122588", 113);
        mFileIndex.put("/external/file/122587", 114);
        mFileIndex.put("/external/file/122586", 115);
        mFileIndex.put("/external/file/122585", 116);
        mFileIndex.put("/external/file/122584", 117);
        mFileIndex.put("/external/file/122583", 118);
        mFileIndex.put("/external/file/122582", 119);
        mFileIndex.put("/external/file/122581", 120);
        mFileIndex.put("/external/file/122580", 121);
        mFileIndex.put("/external/file/122579", 122);
        mFileIndex.put("/external/file/122578", 123);
        mFileIndex.put("/external/file/122577", 124);
        mFileIndex.put("/external/file/122576", 125);
        mFileIndex.put("/external/file/122575", 126);
        mFileIndex.put("/external/file/122574", 127);
        mFileIndex.put("/external/file/122573", 128);
        mFileIndex.put("/external/file/122572", 129);
        mFileIndex.put("/external/file/122571", 130);
        mFileIndex.put("/external/file/122570", 131);
        mFileIndex.put("/external/file/122569", 132);
        mFileIndex.put("/external/file/122568", 133);
        mFileIndex.put("/external/file/122567", 134);
        mFileIndex.put("/external/file/122566", 135);
        mFileIndex.put("/external/file/122565", 136);
        mFileIndex.put("/external/file/122564", 137);
        mFileIndex.put("/external/file/122563", 138);
        mFileIndex.put("/external/file/122562", 139);
        mFileIndex.put("/external/file/122561", 140);
        mFileIndex.put("/external/file/122534", 141);
        mFileIndex.put("/external/file/122528", 142);
        mFileIndex.put("/external/file/122526", 143);
        mFileIndex.put("/external/file/122527", 144);
        mFileIndex.put("/external/file/122525", 145);
        mFileIndex.put("/external/file/122524", 146);
        mFileIndex.put("/external/file/122523", 147);
        mFileIndex.put("/external/file/122522", 148);
        mFileIndex.put("/external/file/122521", 149);
        mFileIndex.put("/external/file/122520", 150);
        mFileIndex.put("/external/file/122519", 151);
        mFileIndex.put("/external/file/122518", 152);
        mFileIndex.put("/external/file/122517", 153);
        mFileIndex.put("/external/file/122516", 154);
        mFileIndex.put("/external/file/122515", 155);
        mFileIndex.put("/external/file/122514", 156);
        mFileIndex.put("/external/file/122513", 157);
        mFileIndex.put("/external/file/122512", 158);
        mFileIndex.put("/external/file/122511", 159);
        mFileIndex.put("/external/file/122510", 160);
        mFileIndex.put("/external/file/122509", 161);
        mFileIndex.put("/external/file/122508", 162);
        mFileIndex.put("/external/file/122507", 163);
        mFileIndex.put("/external/file/122506", 164);
        mFileIndex.put("/external/file/122505", 165);
        mFileIndex.put("/external/file/122504", 166);
        mFileIndex.put("/external/file/122503", 167);
        mFileIndex.put("/external/file/122502", 168);
        mFileIndex.put("/external/file/122501", 169);
        mFileIndex.put("/external/file/122500", 170);
        mFileIndex.put("/external/file/122499", 171);
        mFileIndex.put("/external/file/122498", 172);
        mFileIndex.put("/external/file/122497", 173);
        mFileIndex.put("/external/file/122496", 174);
        mFileIndex.put("/external/file/122495", 175);
        mFileIndex.put("/external/file/122494", 176);
        mFileIndex.put("/external/file/122493", 177);
        mFileIndex.put("/external/file/122492", 178);
        mFileIndex.put("/external/file/122491", 179);
        mFileIndex.put("/external/file/122490", 180);
        mFileIndex.put("/external/file/122489", 181);
        mFileIndex.put("/external/file/122488", 182);
        mFileIndex.put("/external/file/122487", 183);
        mFileIndex.put("/external/file/122486", 184);
        mFileIndex.put("/external/file/122485", 185);
        mFileIndex.put("/external/file/122484", 186);
        mFileIndex.put("/external/file/122483", 187);
        mFileIndex.put("/external/file/122482", 188);
        mFileIndex.put("/external/file/122481", 189);
        mFileIndex.put("/external/file/122480", 190);
        mFileIndex.put("/external/file/122479", 191);
        mFileIndex.put("/external/file/122478", 192);
        mFileIndex.put("/external/file/122477", 193);
        mFileIndex.put("/external/file/122476", 194);
        mFileIndex.put("/external/file/122475", 195);
        mFileIndex.put("/external/file/122474", 196);
        mFileIndex.put("/external/file/122473", 197);
        mFileIndex.put("/external/file/122472", 198);
        mFileIndex.put("/external/file/122471", 199);
        mFileIndex.put("/external/file/122470", 200);
        mFileIndex.put("/external/file/122469", 201);
        mFileIndex.put("/external/file/122468", 202);
        mFileIndex.put("/external/file/122467", 203);
        mFileIndex.put("/external/file/122466", 204);
        mFileIndex.put("/external/file/122465", 205);
        mFileIndex.put("/external/file/122464", 206);
        mFileIndex.put("/external/file/122463", 207);
        mFileIndex.put("/external/file/122462", 208);
        mFileIndex.put("/external/file/122461", 209);
        mFileIndex.put("/external/file/122460", 210);
        mFileIndex.put("/external/file/122459", 211);
        mFileIndex.put("/external/file/122458", 212);
        mFileIndex.put("/external/file/122457", 213);
        mFileIndex.put("/external/file/122456", 214);
        mFileIndex.put("/external/file/122455", 215);
        mFileIndex.put("/external/file/122454", 216);
        mFileIndex.put("/external/file/122453", 217);
        mFileIndex.put("/external/file/122452", 218);
        mFileIndex.put("/external/file/122451", 219);
        mFileIndex.put("/external/file/122450", 220);
        mFileIndex.put("/external/file/122449", 221);
        mFileIndex.put("/external/file/122448", 222);
        mFileIndex.put("/external/file/122447", 223);
        mFileIndex.put("/external/file/122446", 224);
        mFileIndex.put("/external/file/122445", 225);
        mFileIndex.put("/external/file/122444", 226);
        mFileIndex.put("/external/file/122443", 227);
        mFileIndex.put("/external/file/122442", 228);
        mFileIndex.put("/external/file/122441", 229);
        mFileIndex.put("/external/file/122440", 230);
        mFileIndex.put("/external/file/122439", 231);
        mFileIndex.put("/external/file/122438", 232);
        mFileIndex.put("/external/file/122437", 233);
        mFileIndex.put("/external/file/122436", 234);
        mFileIndex.put("/external/file/122435", 235);
        mFileIndex.put("/external/file/122434", 236);
        mFileIndex.put("/external/file/122428", 237);
        mFileIndex.put("/external/file/122429", 238);
        mFileIndex.put("/external/file/122427", 239);
        mFileIndex.put("/external/file/122426", 240);
        mFileIndex.put("/external/file/122425", 241);
        mFileIndex.put("/external/file/122424", 242);
        mFileIndex.put("/external/file/122423", 243);
        mFileIndex.put("/external/file/122422", 244);
        mFileIndex.put("/external/file/122421", 245);
        mFileIndex.put("/external/file/122420", 246);
        mFileIndex.put("/external/file/122419", 247);
        mFileIndex.put("/external/file/122412", 248);
        mFileIndex.put("/external/file/122413", 249);
        mFileIndex.put("/external/file/122411", 250);
        mFileIndex.put("/external/file/122410", 251);
        mFileIndex.put("/external/file/122409", 252);
        mFileIndex.put("/external/file/122408", 253);
        mFileIndex.put("/external/file/122407", 254);
        mFileIndex.put("/external/file/122406", 255);
        mFileIndex.put("/external/file/122405", 256);
        mFileIndex.put("/external/file/122404", 257);
        mFileIndex.put("/external/file/122403", 258);
        mFileIndex.put("/external/file/122402", 259);
        mFileIndex.put("/external/file/122401", 260);
        mFileIndex.put("/external/file/122400", 261);
        mFileIndex.put("/external/file/122399", 262);
        mFileIndex.put("/external/file/122398", 263);
        mFileIndex.put("/external/file/122397", 264);
        mFileIndex.put("/external/file/122396", 265);
        mFileIndex.put("/external/file/122395", 266);
        mFileIndex.put("/external/file/122394", 267);
        mFileIndex.put("/external/file/122393", 268);
        mFileIndex.put("/external/file/122392", 269);
        mFileIndex.put("/external/file/122391", 270);
        mFileIndex.put("/external/file/122389", 271);
        mFileIndex.put("/external/file/122390", 272);
        mFileIndex.put("/external/file/122388", 273);
        mFileIndex.put("/external/file/122387", 274);
        mFileIndex.put("/external/file/122386", 275);
        mFileIndex.put("/external/file/122385", 276);
        mFileIndex.put("/external/file/122384", 277);
        mFileIndex.put("/external/file/122383", 278);
        mFileIndex.put("/external/file/122382", 279);
        mFileIndex.put("/external/file/122381", 280);
        mFileIndex.put("/external/file/122380", 281);
        mFileIndex.put("/external/file/122379", 282);
        mFileIndex.put("/external/file/122378", 283);
        mFileIndex.put("/external/file/122377", 284);
        mFileIndex.put("/external/file/122376", 285);
        mFileIndex.put("/external/file/122375", 286);
        mFileIndex.put("/external/file/122374", 287);
        mFileIndex.put("/external/file/122373", 288);
        mFileIndex.put("/external/file/122372", 289);
        mFileIndex.put("/external/file/122371", 290);
        mFileIndex.put("/external/file/122370", 291);
        mFileIndex.put("/external/file/122369", 292);
        mFileIndex.put("/external/file/122368", 293);
        mFileIndex.put("/external/file/122367", 294);
        mFileIndex.put("/external/file/122366", 295);
        mFileIndex.put("/external/file/122365", 296);
        mFileIndex.put("/external/file/122364", 297);
        mFileIndex.put("/external/file/122363", 298);
        mFileIndex.put("/external/file/122362", 299);
        mFileIndex.put("/external/file/122361", 300);
        mFileIndex.put("/external/file/122360", 301);
        mFileIndex.put("/external/file/122359", 302);
        mFileIndex.put("/external/file/122358", 303);
        mFileIndex.put("/external/file/122357", 304);
        mFileIndex.put("/external/file/122356", 305);
        mFileIndex.put("/external/file/122355", 306);
        mFileIndex.put("/external/file/122354", 307);
        mFileIndex.put("/external/file/122353", 308);
        mFileIndex.put("/external/file/122348", 309);
        mFileIndex.put("/external/file/122347", 310);
        mFileIndex.put("/external/file/122346", 311);
        mFileIndex.put("/external/file/122343", 312);
        mFileIndex.put("/external/file/122344", 313);
        mFileIndex.put("/external/file/122342", 314);
        mFileIndex.put("/external/file/122341", 315);
        mFileIndex.put("/external/file/122340", 316);
        mFileIndex.put("/external/file/122339", 317);
        mFileIndex.put("/external/file/122338", 318);
        mFileIndex.put("/external/file/122337", 319);
        mFileIndex.put("/external/file/122336", 320);
        mFileIndex.put("/external/file/122335", 321);
        mFileIndex.put("/external/file/122334", 322);
        mFileIndex.put("/external/file/122333", 323);
        mFileIndex.put("/external/file/122332", 324);
        mFileIndex.put("/external/file/122331", 325);
        mFileIndex.put("/external/file/122330", 326);
        mFileIndex.put("/external/file/122329", 327);
        mFileIndex.put("/external/file/122328", 328);
        mFileIndex.put("/external/file/122327", 329);
        mFileIndex.put("/external/file/122326", 330);
        mFileIndex.put("/external/file/122325", 331);
        mFileIndex.put("/external/file/122324", 332);
        mFileIndex.put("/external/file/122323", 333);
        mFileIndex.put("/external/file/122322", 334);
        mFileIndex.put("/external/file/122321", 335);
        mFileIndex.put("/external/file/122320", 336);
        mFileIndex.put("/external/file/122319", 337);
        mFileIndex.put("/external/file/122318", 338);
        mFileIndex.put("/external/file/122317", 339);
        mFileIndex.put("/external/file/122316", 340);
        mFileIndex.put("/external/file/122315", 341);
        mFileIndex.put("/external/file/122314", 342);
        mFileIndex.put("/external/file/122313", 343);
        mFileIndex.put("/external/file/122312", 344);
        mFileIndex.put("/external/file/122311", 345);
        mFileIndex.put("/external/file/122310", 346);
        mFileIndex.put("/external/file/122309", 347);
        mFileIndex.put("/external/file/121580", 348);
        mFileIndex.put("/external/file/121549", 349);
        mFileIndex.put("/external/file/120445", 350);
        mFileIndex.put("/external/file/120409", 351);
        mFileIndex.put("/external/file/118911", 352);
        mFileIndex.put("/external/file/118908", 353);
        mFileIndex.put("/external/file/118907", 354);
        mFileIndex.put("/external/file/117470", 355);
        mFileIndex.put("/external/file/116164", 356);
        mFileIndex.put("/external/file/116130", 357);
        mFileIndex.put("/external/file/116089", 358);
        mFileIndex.put("/external/file/116074", 359);
        mFileIndex.put("/external/file/113396", 360);
        mFileIndex.put("/external/file/111676", 361);
        mFileIndex.put("/external/file/110215", 362);
        mFileIndex.put("/external/file/109237", 363);
        mFileIndex.put("/external/file/109235", 364);
        mFileIndex.put("/external/file/109110", 365);
        mFileIndex.put("/external/file/109109", 366);
        mFileIndex.put("/external/file/107784", 367);
        mFileIndex.put("/external/file/107280", 368);
        mFileIndex.put("/external/file/107268", 369);
        mFileIndex.put("/external/file/107190", 370);
        mFileIndex.put("/external/file/106356", 371);
        mFileIndex.put("/external/file/106355", 372);
        mFileIndex.put("/external/file/100615", 373);
        mFileIndex.put("/external/file/100587", 374);
        mFileIndex.put("/external/file/100476", 375);
        mFileIndex.put("/external/file/95400", 376);
        mFileIndex.put("/external/file/94071", 377);
        mFileIndex.put("/external/file/94070", 378);
        mFileIndex.put("/external/file/91279", 379);
        mFileIndex.put("/external/file/90562", 380);
        mFileIndex.put("/external/file/89757", 381);
        mFileIndex.put("/external/file/89756", 382);
        mFileIndex.put("/external/file/89755", 383);
        mFileIndex.put("/external/file/88434", 384);
        mFileIndex.put("/external/file/88421", 385);
        mFileIndex.put("/external/file/86529", 386);
        mFileIndex.put("/external/file/86111", 387);
        mFileIndex.put("/external/file/85127", 388);
        mFileIndex.put("/external/file/81193", 389);
        mFileIndex.put("/external/file/81171", 390);
        mFileIndex.put("/external/file/80643", 391);
        mFileIndex.put("/external/file/78891", 392);
        mFileIndex.put("/external/file/78890", 393);
        mFileIndex.put("/external/file/78094", 394);
        mFileIndex.put("/external/file/78092", 395);
        mFileIndex.put("/external/file/75159", 396);
        mFileIndex.put("/external/file/75136", 397);
        mFileIndex.put("/external/file/74148", 398);
        mFileIndex.put("/external/file/73302", 399);
        mFileIndex.put("/external/file/73301", 400);
        mFileIndex.put("/external/file/71301", 401);
        mFileIndex.put("/external/file/71300", 402);
        mFileIndex.put("/external/file/67414", 403);
        mFileIndex.put("/external/file/67413", 404);
        mFileIndex.put("/external/file/65220", 405);
        mFileIndex.put("/external/file/65207", 406);
        mFileIndex.put("/external/file/65206", 407);
        mFileIndex.put("/external/file/65092", 408);
        mFileIndex.put("/external/file/60577", 409);
        mFileIndex.put("/external/file/60574", 410);
        mFileIndex.put("/external/file/60573", 411);
        mFileIndex.put("/external/file/57344", 412);
        mFileIndex.put("/external/file/57343", 413);
        mFileIndex.put("/external/file/52380", 414);
        mFileIndex.put("/external/file/52379", 415);
        mFileIndex.put("/external/file/52378", 416);
        mFileIndex.put("/external/file/47401", 417);
        mFileIndex.put("/external/file/47400", 418);
        mFileIndex.put("/external/file/47382", 419);
        mFileIndex.put("/external/file/47374", 420);
        mFileIndex.put("/external/file/46540", 421);
        mFileIndex.put("/external/file/46231", 422);
        mFileIndex.put("/external/file/46230", 423);
        mFileIndex.put("/external/file/45063", 424);
        mFileIndex.put("/external/file/40212", 425);
        mFileIndex.put("/external/file/40208", 426);
        mFileIndex.put("/external/file/40203", 427);
        mFileIndex.put("/external/file/40202", 428);
        mFileIndex.put("/external/file/40167", 429);
        mFileIndex.put("/external/file/37386", 430);
        mFileIndex.put("/external/file/37362", 431);
        mFileIndex.put("/external/file/37313", 432);
        mFileIndex.put("/external/file/37312", 433);
        mFileIndex.put("/external/file/37287", 434);
        mFileIndex.put("/external/file/37286", 435);
        mFileIndex.put("/external/file/36256", 436);
        mFileIndex.put("/external/file/21349", 437);
        mFileIndex.put("/external/file/21348", 438);
        mFileIndex.put("/external/file/21347", 439);
        mFileIndex.put("/external/file/20918", 440);
        mFileIndex.put("/external/file/15252", 441);
        mFileIndex.put("/external/file/15251", 442);
        mFileIndex.put("/external/file/15250", 443);
        mFileIndex.put("/external/file/10202", 444);
        mFileIndex.put("/external/file/6116", 445);
        mFileIndex.put("/external/file/6111", 446);
        mFileIndex.put("/external/file/4327", 447);
        mFileIndex.put("/external/file/4326", 448);
        mFileIndex.put("/external/file/4325", 449);
    }

    /**
     * Retrieves the DCIM/camera directory.
     */
    private String getCameraDirectory() {
        return Environment.DIRECTORY_DCIM + File.separator + SAMPLE_DCIM_SOURCE_SUB_DIRECTORY;
    }

    /**
     * Enumerates (in the background) the image files on disk. Called on a non-UI thread
     * @return A sorted list of images (by last-modified first).
     */
    @Override
    protected List<PickerBitmap> doInBackground() {
        ThreadUtils.assertOnBackgroundThread();

        if (isCancelled()) return null;

        List<PickerBitmap> pickerBitmaps = new ArrayList<>();

        // The DATA column is deprecated in the Android Q SDK. Replaced by relative_path.
        String directoryColumnName =
                BuildInfo.isAtLeastQ() ? "relative_path" : MediaStore.Files.FileColumns.DATA;
        final String[] selectColumns = {
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                directoryColumnName,
        };

        String whereClause = "(" + directoryColumnName + " LIKE ? OR " + directoryColumnName
                + " LIKE ? OR " + directoryColumnName + " LIKE ?) AND " + directoryColumnName
                + " NOT LIKE ?";
        String additionalClause = "";
        if (mIncludeImages) {
            additionalClause = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
        }
        if (mIncludeVideos) {
            if (mIncludeImages) additionalClause += " OR ";
            additionalClause += MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
        }
        if (!additionalClause.isEmpty()) whereClause += " AND (" + additionalClause + ")";

        String cameraDir = getCameraDirectory();
        String picturesDir = Environment.DIRECTORY_PICTURES;
        String downloadsDir = Environment.DIRECTORY_DOWNLOADS;
        String screenshotsDir = Environment.DIRECTORY_PICTURES + "/Screenshots";
        if (!BuildInfo.isAtLeastQ()) {
            cameraDir = Environment.getExternalStoragePublicDirectory(cameraDir).toString();
            picturesDir = Environment.getExternalStoragePublicDirectory(picturesDir).toString();
            downloadsDir = Environment.getExternalStoragePublicDirectory(downloadsDir).toString();
            screenshotsDir =
                    Environment.getExternalStoragePublicDirectory(screenshotsDir).toString();
        }

        String[] whereArgs = new String[] {
                // Include:
                cameraDir + "%",
                picturesDir + "%",
                downloadsDir + "%",
                // Exclude low-quality sources, such as the screenshots directory:
                screenshotsDir + "%",
        };

        final String orderBy = MediaStore.MediaColumns.DATE_ADDED + " DESC";

        Uri contentUri = MediaStore.Files.getContentUri("external");
        Cursor imageCursor =
                createImageCursor(contentUri, selectColumns, whereClause, whereArgs, orderBy);
        if (imageCursor == null) {
            Log.e(TAG, "Content Resolver query() returned null");
            return null;
        }

        Log.i(TAG,
                "Found " + imageCursor.getCount() + " media files, when requesting columns: "
                        + Arrays.toString(selectColumns) + ", with WHERE " + whereClause
                        + ", params: " + Arrays.toString(whereArgs));

        while (imageCursor.moveToNext()) {
            int mimeTypeIndex = imageCursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE);
            String mimeType = imageCursor.getString(mimeTypeIndex);
            if (!mFilter.accept(null, mimeType)) continue;

            int dateTakenIndex =
                    imageCursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED);
            int idIndex = imageCursor.getColumnIndex(MediaStore.Files.FileColumns._ID);
            Uri uri = ContentUris.withAppendedId(contentUri, imageCursor.getInt(idIndex));
            long dateTaken = imageCursor.getLong(dateTakenIndex);

            @PickerBitmap.TileTypes
            int type = PickerBitmap.TileTypes.PICTURE;
            if (mimeType.startsWith("video/")) type = PickerBitmap.TileTypes.VIDEO;

            pickerBitmaps.add(new PickerBitmap(uri, dateTaken, type));
        }
        imageCursor.close();

        pickerBitmaps.add(0, new PickerBitmap(null, 0, PickerBitmap.TileTypes.GALLERY));
        if (shouldShowCameraTile()) {
            pickerBitmaps.add(0, new PickerBitmap(null, 0, PickerBitmap.TileTypes.CAMERA));
        }

        return pickerBitmaps;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        mCallback.filesEnumeratedCallback(null);
    }

    /**
     * Communicates the results back to the client. Called on the UI thread.
     * @param files The resulting list of files on disk.
     */
    @Override
    protected void onPostExecute(List<PickerBitmap> files) {
        if (isCancelled()) {
            return;
        }

        mCallback.filesEnumeratedCallback(files);
    }

    /**
     * Creates a cursor containing the image files to show. Can be overridden in tests to provide
     * fake data.
     */
    protected Cursor createImageCursor(Uri contentUri, String[] selectColumns, String whereClause,
            String[] whereArgs, String orderBy) {
        return mContentResolver.query(contentUri, selectColumns, whereClause, whereArgs, orderBy);
    }

    /**
     * Returns whether to include the Camera tile also.
     */
    protected boolean shouldShowCameraTile() {
        return true;
        /* Not required in Android-Studio project.
        boolean hasCameraAppAvailable =
                mWindowAndroid.canResolveActivity(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
        boolean hasOrCanRequestCameraPermission =
                (mWindowAndroid.hasPermission(Manifest.permission.CAMERA)
                        || mWindowAndroid.canRequestPermission(Manifest.permission.CAMERA));
        return hasCameraAppAvailable && hasOrCanRequestCameraPermission;
        */
    }
}
