   function volume_up()
   {
	var previous =  document.video1.get_volume();
        var newvolume = previous + 10;
	if( newvolume > 200 )  newvolume = 200;
	document.video1.set_volume( newvolume );
	var volume = document.getElementById("volume_status");
        volume.innerHTML = document.video1.get_volume() + " %";
   }

   function volume_down()
   {
	var previous =  document.video1.get_volume();
        var newvolume = previous - 10;
	if( newvolume < 0 )  newvolume = 0;
	document.video1.set_volume( newvolume );
	var volume = document.getElementById("volume_status");
        volume.innerHTML = document.video1.get_volume() + " %";
   }

   function status()
   {
        var play_status = document.getElementById("play_status");
        var time = document.getElementById("time");
        var length = document.getElementById("length");
	var hours = 0;
	var minutes = 0;
	var seconds = 0;

        play_status.innerHTML = document.video1.isplaying() ? "Playing" : "Not playing";
        if( document.video1.isplaying() == true )
        {
	        got_time = document.video1.get_time();
		hours = Math.floor(got_time/ 3600);
		minutes = Math.floor((got_time/60) % 60);
		seconds = got_time % 60;
		if ( hours < 10 ) hours = "0" + hours;
		if ( minutes < 10 ) minutes = "0" + minutes;
		if ( seconds < 10 ) seconds = "0" + seconds;
		time.innerHTML = hours+":"+minutes+":"+seconds;

	        got_length = document.video1.get_length();
		hours = Math.floor(got_length/ 3600);
		minutes = Math.floor((got_length/60) % 60);
		seconds = got_length % 60;
		if ( hours < 10 ) hours = "0" + hours;
		if ( minutes < 10 ) minutes = "0" + minutes;
		if ( seconds < 10 ) seconds = "0" + seconds;
                length.innerHTML = hours+":"+minutes+":"+seconds;
        }
        else
        {  
                time.innerHTML = "--:--:--";
                length.innerHTML = "--:--:--";
        }
        setTimeout("status()", 1000 );
    }

    function play_selected()
    {
	select = document.getElementById("item");
	set_item( select.value );
  	document.video1.play();
	document.getElementById('XVideo').set_bool_variable('xvideo-shm',false);
    }

    function set_item( name)
    {
       document.video1.stop();
       document.video1.clear_playlist();
       document.video1.add_item( name );
    }
