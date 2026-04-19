<?php

namespace App\Support;

use App\Contracts\ProvidesProductProperties;

class ProductPropertyProvider implements ProvidesProductProperties
{
    public function getProductProperties(): array
    {
        return [];
    }
}
