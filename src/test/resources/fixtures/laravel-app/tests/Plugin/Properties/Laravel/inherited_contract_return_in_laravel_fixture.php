<?php

use App\Support\ProductPropertyProvider;

$title = (new ProductPropertyProvider())->getProductProperties()['title'];
