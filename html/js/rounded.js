var style = 'b.rtop, b.rbottom{display:block;background: #2e3436}b.rbottom{margin-top:-16px;padding-bottom:16px;}b.rtop b, b.rbottom b{display:block;height: 1px;overflow: hidden; background: #272c2d}b.r1{margin: 0 5px}b.r2{margin: 0 3px}b.r3{margin: 0 2px}b.rtop b.r4, b.rbottom b.r4{margin: 0 1px;height: 2px}';

function roundcorners() {
	var st = document.createElement('style');
	st.type = 'text/css';
	var t = document.createTextNode(style)
	st.appendChild(t);
	document.body.appendChild(st);

	var divs = document.getElementsByClassName('msg')
	for (var i = 0; i < divs.length; i++)
	{
		var cnode=divs[i];
		var it=document.createElement('b');
		it.className='rtop';
		var ib=document.createElement('b');
		ib.className='rbottom';

		var i1=document.createElement('b');
		var i2=document.createElement('b');
		var i3=document.createElement('b');
		var i4=document.createElement('b');
		i1.className='r1';
		i2.className='r2';
		i3.className='r3';
		i4.className='r4';

		var it1=document.createElement('b');
		var it2=document.createElement('b');
		var it3=document.createElement('b');
		var it4=document.createElement('b');
		it1.className='r1';
		it2.className='r2';
		it3.className='r3';
		it4.className='r4';


		it.appendChild(it1);
		it.appendChild(it2);
		it.appendChild(it3);
		it.appendChild(it4);

		ib.appendChild(i4);
		ib.appendChild(i3);
		ib.appendChild(i2);
		ib.appendChild(i1);

		var pnode=cnode.parentNode;
		cnode.parentNode.insertBefore(it,cnode);
		cnode.parentNode.insertBefore(ib,cnode.nextSibling);
	}
}

if (window.opera!=null)
{
    document.addEventListener('DOMContentLoaded', roundcorners, false);
}
